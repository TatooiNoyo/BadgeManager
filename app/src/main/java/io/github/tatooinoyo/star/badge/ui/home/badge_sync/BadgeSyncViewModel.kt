package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom

class BadgeSyncViewModel(application: Application) : AndroidViewModel(application) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private class Session(val sender: Boolean, val code: String, val scope: CoroutineScope) {
        var listener: UdpListener? = null
        var broadcaster: UdpBroadcaster? = null
        var client: TcpClient? = null
        var server: TcpServer? = null
        var channel: SecureChannel? = null
        var timeout: Job? = null
        var payloadJob: Job? = null
        var pendingBadges: List<Badge>? = null
        var payloadReceived = false
    }

    @Volatile private var current: Session? = null

    private fun str(id: Int): String = getApplication<Application>().getString(id)
    private fun message(code: SyncErrorCode): String = str(when (code) {
        SyncErrorCode.PORT_IN_USE -> R.string.sync_error_port_in_use
        SyncErrorCode.NETWORK_UNAVAILABLE -> R.string.sync_error_network
        SyncErrorCode.CONNECT_TIMEOUT -> R.string.sync_error_connect_timeout
        SyncErrorCode.HANDSHAKE_FAILED -> R.string.sync_handshake_failed
        SyncErrorCode.TRANSFER_INTERRUPTED -> R.string.sync_error_transfer
        SyncErrorCode.PEER_DISCONNECTED -> R.string.sync_error_peer_disconnected
        SyncErrorCode.IMPORT_FAILED -> R.string.sync_import_failed
        SyncErrorCode.VERSION_INCOMPATIBLE -> R.string.sync_error_version
        SyncErrorCode.ACK_TIMEOUT -> R.string.sync_error_ack_timeout
        SyncErrorCode.ACK_REJECTED -> R.string.sync_error_ack_rejected
        SyncErrorCode.TOO_MANY_HANDSHAKE_FAILURES -> R.string.sync_error_too_many_handshakes
        SyncErrorCode.EMPTY_PAYLOAD -> R.string.share_import_empty
    })

    private fun begin(sender: Boolean, code: String): Session? {
        if (current != null || _syncState.value != SyncState.Idle) return null
        return Session(sender, code, CoroutineScope(
            viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext[Job]),
        )).also { current = it }
    }

    // All state transitions and timers run on Main; callbacks carry their owning session.
    private fun post(session: Session, action: suspend () -> Unit) {
        session.scope.launch { if (current === session) action() }
    }

    private fun timeout(session: Session, millis: Int, code: SyncErrorCode) {
        session.timeout?.cancel()
        session.timeout = session.scope.launch {
            delay(millis.toLong())
            fail(session, code)
        }
    }

    private fun fail(session: Session, code: SyncErrorCode, cause: Throwable? = null) {
        if (current !== session) return
        val detail = cause?.let { Log.getStackTraceString(it) }
        _syncState.value = if (session.sender) SyncState.Sender.Error(session.code, message(code), detail)
        else SyncState.Receiver.Error(message(code), detail)
        release(session)
    }

    private fun channelError(session: Session, error: Exception) = post(session) {
        // Once confirmation starts, local transaction outcome decides the receiver result.
        if (_syncState.value != SyncState.Receiver.Importing && _syncState.value != SyncState.Receiver.Success &&
            _syncState.value !is SyncState.Receiver.Error) {
            fail(session, (error as? SyncProtocolException)?.code ?: SyncErrorCode.TRANSFER_INTERRUPTED, error)
        }
    }

    private fun acceptChannel(session: Session, channel: SecureChannel): Boolean = synchronized(session) {
        if (current !== session || session.channel != null) {
            channel.disconnect(notifyPeer = false)
            false
        } else {
            session.channel = channel
            true
        }
    }

    fun startSenderMode() {
        val code = (SecureRandom().nextInt(900_000) + 100_000).toString()
        val session = begin(true, code) ?: return
        _syncState.value = SyncState.Sender.Ready(code)
        timeout(session, SyncProtocol.DISCOVERY_TIMEOUT_MS, SyncErrorCode.CONNECT_TIMEOUT)
        session.scope.launch {
            try {
                val (json, fingerprint) = withContext(Dispatchers.Default) {
                    val badges = BadgeRepository.getAllBadgesSnapshot()
                    if (badges.isEmpty()) throw SyncProtocolException(SyncErrorCode.EMPTY_PAYLOAD)
                    val json = Gson().toJson(badges)
                    BadgeImportValidator.parse(json)
                    json to SyncProtocol.discoveryFingerprint(code)
                }
                session.listener = UdpListener(
                    expectedFingerprint = fingerprint,
                    onDeviceFound = { ip, port -> post(session) {
                        if (_syncState.value is SyncState.Sender.Ready) {
                            _syncState.value = SyncState.Sender.Handshaking(code, ip)
                            connectSender(session, ip, port, json)
                        }
                    } },
                    onError = { error -> post(session) { fail(session, error) } },
                ).also { it.start() }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fail(session, (e as? SyncProtocolException)?.code ?: SyncErrorCode.TRANSFER_INTERRUPTED, e) }
        }
    }

    private fun connectSender(session: Session, ip: String, port: Int, json: String) {
        session.scope.launch {
            val client = TcpClient(ip, port, session.code)
            session.client = client
            when (val result = client.connectAndGetChannel()) {
                is ConnectResult.Fail -> {
                    client.close()
                    session.client = null
                    // A stale or forged discovery must not consume the entire sharing session.
                    _syncState.value = SyncState.Sender.Ready(session.code)
                }
                is ConnectResult.Ok -> {
                    val channel = result.channel
                    if (!acceptChannel(session, channel)) return@launch
                    session.listener?.stop()
                    session.listener = null
                    _syncState.value = SyncState.Sender.Sending(session.code)
                    timeout(session, SyncProtocol.TRANSFER_TIMEOUT_MS, SyncErrorCode.TRANSFER_INTERRUPTED)
                    channel.startReceiving(
                        onData = { channelError(session, java.io.IOException("Unexpected data")) },
                        onPayloadReceived = { post(session) {
                            session.payloadReceived = true
                            timeout(session, SyncProtocol.CONFIRM_TIMEOUT_MS, SyncErrorCode.ACK_TIMEOUT)
                        } },
                        onAck = { ok -> post(session) {
                            if (!session.payloadReceived) {
                                fail(session, SyncErrorCode.TRANSFER_INTERRUPTED)
                                return@post
                            }
                            _syncState.value = if (ok) SyncState.Sender.Success(session.code)
                            else SyncState.Sender.Error(session.code, message(SyncErrorCode.ACK_REJECTED))
                            release(session)
                        } },
                        onDisconnect = { post(session) { fail(session, SyncErrorCode.PEER_DISCONNECTED) } },
                        onError = { channelError(session, it) },
                    )
                    channel.sendData(json,
                        onSendSuccess = { post(session) {
                            if (!session.payloadReceived) timeout(session, SyncProtocol.ACK_TIMEOUT_MS, SyncErrorCode.ACK_TIMEOUT)
                        } },
                        onSendError = { channelError(session, it) },
                    )
                }
            }
        }
    }

    fun startReceiverMode(inputCode: String) {
        if (_syncState.value != SyncState.Idle) return
        if (!inputCode.matches(Regex("[0-9]{6}"))) {
            _syncState.value = SyncState.Receiver.Error(message(SyncErrorCode.HANDSHAKE_FAILED))
            return
        }
        val session = begin(false, inputCode) ?: return
        _syncState.value = SyncState.Receiver.Searching
        timeout(session, SyncProtocol.DISCOVERY_TIMEOUT_MS, SyncErrorCode.CONNECT_TIMEOUT)
        session.broadcaster = UdpBroadcaster(shareCode = inputCode, onError = { error ->
            post(session) { if (_syncState.value == SyncState.Receiver.Searching) fail(session, error) }
        })
        session.server = TcpServer(inputCode).also { server ->
            server.start(
                onServerReady = { port -> post(session) { session.broadcaster?.start(port) } },
                onChannelReady = { channel ->
                    if (acceptChannel(session, channel)) post(session) { receive(session, channel) }
                },
                onError = { code, cause -> post(session) {
                    if (code != TcpServer.ERROR_HANDSHAKE_FAILED) fail(session, SyncErrorCode.NETWORK_UNAVAILABLE, cause)
                } },
            )
        }
    }

    private fun receive(session: Session, channel: SecureChannel) {
        session.broadcaster?.stop()
        session.broadcaster = null
        _syncState.value = SyncState.Receiver.Receiving(channel.remoteAddress)
        timeout(session, SyncProtocol.TRANSFER_TIMEOUT_MS, SyncErrorCode.TRANSFER_INTERRUPTED)
        channel.startReceiving(
            onData = { json -> post(session) {
                if (_syncState.value !is SyncState.Receiver.Receiving || session.payloadJob != null) {
                    fail(session, SyncErrorCode.TRANSFER_INTERRUPTED)
                } else {
                    session.payloadJob = session.scope.launch {
                        try {
                            val badges = withContext(Dispatchers.Default) { BadgeImportValidator.parse(json) }
                            session.pendingBadges = badges
                            _syncState.value = SyncState.Receiver.Confirming(badges.size)
                            timeout(session, SyncProtocol.CONFIRM_TIMEOUT_MS, SyncErrorCode.ACK_TIMEOUT)
                            channel.sendPayloadReceived(onSendError = { channelError(session, it) })
                        } catch (e: CancellationException) { throw e }
                        catch (e: Exception) { fail(session, SyncErrorCode.IMPORT_FAILED, e) }
                    }
                }
            } },
            onAck = { channelError(session, java.io.IOException("Unexpected acknowledgement")) },
            onPayloadReceived = { channelError(session, java.io.IOException("Unexpected receipt")) },
            onDisconnect = { channelError(session, java.io.IOException("Peer disconnected")) },
            onError = { channelError(session, it) },
        )
    }

    fun confirmImport() {
        val session = current ?: return
        if (_syncState.value !is SyncState.Receiver.Confirming) return
        val badges = session.pendingBadges ?: return
        session.pendingBadges = null
        _syncState.value = SyncState.Receiver.Importing
        session.timeout?.cancel()
        session.scope.launch {
            try {
                withContext(Dispatchers.IO) { backupLocalBadgesToCache() }
                BadgeRepository.restoreBadges(badges)
                _syncState.value = SyncState.Receiver.Success
                session.channel?.sendAck(true,
                    onSendSuccess = { post(session) { release(session) } },
                    onSendError = { post(session) { release(session) } },
                ) ?: release(session)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                Log.e("BadgeSync", "Import failed; existing data was preserved", e)
                _syncState.value = SyncState.Receiver.Error(message(SyncErrorCode.IMPORT_FAILED), Log.getStackTraceString(e))
                session.channel?.sendAck(false,
                    onSendSuccess = { post(session) { release(session) } },
                    onSendError = { post(session) { release(session) } },
                ) ?: release(session)
            }
        }
    }

    fun cancelImport() {
        val session = current ?: return
        if (_syncState.value !is SyncState.Receiver.Confirming) return
        session.pendingBadges = null
        session.timeout?.cancel()
        _syncState.value = SyncState.Receiver.Error(message(SyncErrorCode.ACK_REJECTED))
        session.channel?.sendAck(false,
            onSendSuccess = { post(session) { release(session) } },
            onSendError = { post(session) { release(session) } },
        ) ?: release(session)
    }

    fun stopSenderMode() = stopMode()
    fun stopReceiverMode() = stopMode()

    private fun stopMode() {
        // Confirmation starts one atomic operation; there is no ambiguous mid-commit cancel.
        if (_syncState.value == SyncState.Receiver.Importing) return
        current?.let { release(it) }
        _syncState.value = SyncState.Idle
    }

    private fun release(session: Session) {
        if (current !== session) return
        current = null
        session.scope.cancel()
        session.listener?.stop()
        session.broadcaster?.stop()
        session.server?.stop()
        synchronized(session) { session.channel?.disconnect("closed") }
        // The client socket belongs to the channel after handoff; let its bounded close send the final frame.
        if (session.channel == null) session.client?.close()
        session.pendingBadges = null
    }

    private suspend fun backupLocalBadgesToCache() {
        val dir = getApplication<Application>().cacheDir
        val name = "lan_sync_backup_${System.currentTimeMillis()}_${System.nanoTime()}.json"
        val temporary = File(dir, "$name.tmp")
        try {
            temporary.writeText(Gson().toJson(BadgeRepository.getAllBadgesSnapshot()))
            check(temporary.renameTo(File(dir, name))) { "Cannot save backup" }
        } finally { temporary.delete() }
        dir.listFiles { file -> file.name.startsWith("lan_sync_backup_") && file.extension == "json" }
            ?.sortedByDescending { it.lastModified() }?.drop(5)?.forEach { it.delete() }
    }

    override fun onCleared() {
        current?.let { release(it) }
        super.onCleared()
    }
}
