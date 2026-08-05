package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom

class BadgeSyncViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private var udpListener: UdpListener? = null
    private var udpBroadcaster: UdpBroadcaster? = null
    private var tcpClient: TcpClient? = null
    private var tcpServer: TcpServer? = null
    private var tcpChannel: SecureChannel? = null
    private var senderConnectJob: Job? = null
    private var ackTimeoutJob: Job? = null

    private var pendingImportJson: String? = null
    private var currentShareCode: String? = null
    private var receiverShareCode: String? = null

    private fun str(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    private fun detailOf(e: Throwable): String = buildString {
        append(e::class.java.name)
        append(": ")
        appendLine(e.message ?: "")
        appendLine()
        append(Log.getStackTraceString(e))
    }

    private fun mapSyncError(code: SyncErrorCode): String = when (code) {
        SyncErrorCode.PORT_IN_USE -> str(R.string.sync_error_port_in_use)
        SyncErrorCode.NETWORK_UNAVAILABLE -> str(R.string.sync_error_network)
        SyncErrorCode.CONNECT_TIMEOUT -> str(R.string.sync_error_connect_timeout)
        SyncErrorCode.HANDSHAKE_FAILED -> str(R.string.sync_handshake_failed)
        SyncErrorCode.TRANSFER_INTERRUPTED -> str(R.string.sync_error_transfer)
        SyncErrorCode.PEER_DISCONNECTED -> str(R.string.sync_error_peer_disconnected)
        SyncErrorCode.IMPORT_FAILED -> str(R.string.sync_import_failed)
        SyncErrorCode.VERSION_INCOMPATIBLE -> str(R.string.sync_error_version)
        SyncErrorCode.ACK_TIMEOUT -> str(R.string.sync_error_ack_timeout)
        SyncErrorCode.ACK_REJECTED -> str(R.string.sync_error_ack_rejected)
        SyncErrorCode.TOO_MANY_HANDSHAKE_FAILURES -> str(R.string.sync_error_too_many_handshakes)
        SyncErrorCode.EMPTY_PAYLOAD -> str(R.string.share_import_empty)
    }

    private fun mapTcpServerError(codeOrMessage: String): String = when (codeOrMessage) {
        TcpServer.ERROR_HANDSHAKE_FAILED -> mapSyncError(SyncErrorCode.HANDSHAKE_FAILED)
        TcpServer.ERROR_LISTENER_DISCONNECTED -> str(R.string.sync_listener_disconnected)
        TcpServer.ERROR_TOO_MANY_HANDSHAKE_FAILURES ->
            mapSyncError(SyncErrorCode.TOO_MANY_HANDSHAKE_FAILURES)
        SyncErrorCode.NETWORK_UNAVAILABLE.name -> mapSyncError(SyncErrorCode.NETWORK_UNAVAILABLE)
        else -> mapSyncError(SyncErrorCode.TRANSFER_INTERRUPTED)
    }

    private fun isReceiverTerminal(state: SyncState): Boolean =
        state is SyncState.Receiver.Success || state is SyncState.Receiver.Error

    private fun isSenderTerminal(state: SyncState): Boolean =
        state is SyncState.Sender.Success || state is SyncState.Sender.Error

    private fun generateShareCode(): String {
        val n = SecureRandom().nextInt(900_000) + 100_000
        return n.toString()
    }

    fun startSenderMode() {
        viewModelScope.launch {
            val code = generateShareCode()
            currentShareCode = code
            try {
                _syncState.value = SyncState.Sender.Ready(code)
                val badgeJson = exportBadgesToJson()
                val expectedFp = SyncProtocol.discoveryFingerprint(code)

                udpListener = UdpListener(
                    expectedFingerprint = expectedFp,
                    onDeviceFound = { ip, targetPort ->
                        if (_syncState.value !is SyncState.Sender.Ready) {
                            return@UdpListener
                        }
                        _syncState.value = SyncState.Sender.Handshaking(code, ip)
                        Log.d(TAG, "onDeviceFound: $ip:$targetPort")

                        senderConnectJob?.cancel()
                        senderConnectJob = viewModelScope.launch {
                            val client = TcpClient(ip, targetPort = targetPort, psk6 = code)
                            tcpClient = client
                            when (val result = client.connectAndGetChannel()) {
                                is ConnectResult.Fail -> {
                                    _syncState.value = SyncState.Sender.Error(
                                        code,
                                        mapSyncError(result.code),
                                        result.cause?.let { detailOf(it) },
                                    )
                                    stopSenderConn()
                                    return@launch
                                }
                                is ConnectResult.Ok -> {
                                    val channel = result.channel
                                    tcpChannel = channel
                                    udpListener?.stop()
                                    udpListener = null
                                    _syncState.value = SyncState.Sender.Sending(code)

                                    var ackReceived = false
                                    var payloadAcked = false
                                    channel.startReceiving(
                                        onData = { /* sender only expects received/ack */ },
                                        onPayloadReceived = {
                                            payloadAcked = true
                                            ackTimeoutJob?.cancel()
                                            ackTimeoutJob = viewModelScope.launch {
                                                delay(SyncProtocol.CONFIRM_TIMEOUT_MS.toLong())
                                                if (!ackReceived &&
                                                    _syncState.value is SyncState.Sender.Sending
                                                ) {
                                                    _syncState.value = SyncState.Sender.Error(
                                                        code,
                                                        mapSyncError(SyncErrorCode.ACK_TIMEOUT),
                                                    )
                                                    stopSenderConn()
                                                }
                                            }
                                        },
                                        onAck = { ok ->
                                            ackTimeoutJob?.cancel()
                                            ackReceived = true
                                            if (ok) {
                                                _syncState.value = SyncState.Sender.Success(code)
                                            } else {
                                                _syncState.value = SyncState.Sender.Error(
                                                    code,
                                                    mapSyncError(SyncErrorCode.ACK_REJECTED),
                                                )
                                            }
                                            channel.disconnect(
                                                reason = if (ok) "transfer_ok" else "ack_rejected",
                                                notifyPeer = true,
                                            )
                                        },
                                        onDisconnect = {
                                            if (!ackReceived &&
                                                _syncState.value is SyncState.Sender.Sending
                                            ) {
                                                _syncState.value = SyncState.Sender.Error(
                                                    code,
                                                    mapSyncError(SyncErrorCode.PEER_DISCONNECTED),
                                                )
                                            }
                                        },
                                        onError = { err ->
                                            ackTimeoutJob?.cancel()
                                            if (!ackReceived && !isSenderTerminal(_syncState.value)) {
                                                val message = when (err) {
                                                    is SyncProtocolException -> mapSyncError(err.code)
                                                    else -> mapSyncError(SyncErrorCode.TRANSFER_INTERRUPTED)
                                                }
                                                _syncState.value = SyncState.Sender.Error(
                                                    code,
                                                    message,
                                                    detailOf(err),
                                                )
                                            }
                                            Log.w(TAG, "Sender receive error", err)
                                        },
                                    )

                                    channel.sendData(
                                        badgeJson,
                                        onSendError = { err ->
                                            if (!ackReceived && !isSenderTerminal(_syncState.value)) {
                                                _syncState.value = SyncState.Sender.Error(
                                                    code,
                                                    mapSyncError(SyncErrorCode.TRANSFER_INTERRUPTED),
                                                    detailOf(err),
                                                )
                                                stopSenderConn()
                                            }
                                        },
                                    )

                                    ackTimeoutJob = viewModelScope.launch {
                                        delay(SyncProtocol.ACK_TIMEOUT_MS.toLong())
                                        if (!payloadAcked && !ackReceived &&
                                            _syncState.value is SyncState.Sender.Sending
                                        ) {
                                            _syncState.value = SyncState.Sender.Error(
                                                code,
                                                mapSyncError(SyncErrorCode.ACK_TIMEOUT),
                                            )
                                            stopSenderConn()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onError = { errCode ->
                        _syncState.value = SyncState.Sender.Error(
                            currentShareCode ?: code,
                            mapSyncError(errCode),
                        )
                        stopSenderConn()
                    },
                )
                udpListener?.start()
            } catch (e: Exception) {
                Log.w(TAG, "startSenderMode failed", e)
                _syncState.value = SyncState.Sender.Error(
                    code,
                    str(R.string.sync_send_error, e.message ?: ""),
                    detailOf(e),
                )
                stopSenderConn()
            }
        }
    }

    fun stopSenderConn() {
        ackTimeoutJob?.cancel()
        ackTimeoutJob = null
        senderConnectJob?.cancel()
        senderConnectJob = null

        udpListener?.stop()
        udpListener = null

        tcpChannel?.disconnect("closed")
        tcpChannel = null

        tcpClient?.close()
        tcpClient = null
    }

    fun stopSenderMode() {
        stopSenderConn()
        currentShareCode = null
        _syncState.value = SyncState.Idle
    }

    fun startReceiverMode(inputCode: String) {
        receiverShareCode = inputCode
        viewModelScope.launch {
            pendingImportJson = null
            udpBroadcaster = UdpBroadcaster(
                shareCode = inputCode,
                onError = { code ->
                    if (_syncState.value is SyncState.Receiver.Searching) {
                        _syncState.value = SyncState.Receiver.Error(mapSyncError(code))
                        stopReceiverConn()
                    }
                },
            )
            tcpServer = TcpServer(inputCode)
            _syncState.value = SyncState.Receiver.Searching

            tcpServer?.start(
                onServerReady = { port ->
                    udpBroadcaster?.start(port)
                },
                onChannelReady = { channel ->
                    tcpChannel = channel
                    udpBroadcaster?.stop()
                    udpBroadcaster = null
                    _syncState.value = SyncState.Receiver.Receiving(channel.remoteAddress)
                    channel.startReceiving(
                        onData = { badgeJsonStr ->
                            viewModelScope.launch {
                                handleIncomingPayload(badgeJsonStr)
                            }
                        },
                        onAck = { /* receiver sends ack, does not expect one */ },
                        onDisconnect = {
                            when (_syncState.value) {
                                is SyncState.Receiver.Receiving,
                                is SyncState.Receiver.Confirming,
                                -> {
                                    _syncState.value = SyncState.Receiver.Error(
                                        mapSyncError(SyncErrorCode.PEER_DISCONNECTED),
                                    )
                                }
                                else -> Unit
                            }
                        },
                        onError = { err ->
                            Log.w(TAG, "Receiver channel error", err)
                            if (!isReceiverTerminal(_syncState.value)) {
                                val message = when (err) {
                                    is SyncProtocolException -> mapSyncError(err.code)
                                    else -> mapSyncError(SyncErrorCode.TRANSFER_INTERRUPTED)
                                }
                                _syncState.value = SyncState.Receiver.Error(message, detailOf(err))
                                stopReceiverConn()
                            }
                        },
                    )
                },
                onError = { errMsg, cause ->
                    when (errMsg) {
                        TcpServer.ERROR_HANDSHAKE_FAILED -> {
                            Log.w(TAG, "Handshake failed, waiting for another attempt", cause)
                        }
                        TcpServer.ERROR_TOO_MANY_HANDSHAKE_FAILURES -> {
                            _syncState.value = SyncState.Receiver.Error(
                                mapTcpServerError(errMsg),
                                cause?.let { detailOf(it) },
                            )
                            stopReceiverConn()
                        }
                        else -> {
                            if (!isReceiverTerminal(_syncState.value)) {
                                _syncState.value = SyncState.Receiver.Error(
                                    mapTcpServerError(errMsg),
                                    cause?.let { detailOf(it) },
                                )
                            }
                            stopReceiverConn()
                        }
                    }
                },
            )
        }
    }

    private suspend fun handleIncomingPayload(badgeJsonStr: String) {
        val channel = tcpChannel ?: return
        val count = parseBadgeCount(badgeJsonStr)
        pendingImportJson = badgeJsonStr
        channel.sendPayloadReceived(
            onSendError = { err ->
                Log.w(TAG, "Failed to send payload-received ack", err)
                if (!isReceiverTerminal(_syncState.value)) {
                    _syncState.value = SyncState.Receiver.Error(
                        mapSyncError(SyncErrorCode.TRANSFER_INTERRUPTED),
                        detailOf(err),
                    )
                    stopReceiverConn()
                }
            },
        )
        _syncState.value = SyncState.Receiver.Confirming(count)
    }

    fun confirmImport() {
        val json = pendingImportJson ?: return
        val channel = tcpChannel ?: return

        viewModelScope.launch {
            try {
                backupLocalBadgesToCache()
                val success = importBadgesFromJson(json)
                if (success) {
                    channel.sendAck(
                        ok = true,
                        onSendSuccess = {
                            _syncState.value = SyncState.Receiver.Success
                        },
                        onSendError = { err ->
                            Log.e(TAG, "Failed to send import ack", err)
                            // Local import already succeeded; still show success for receiver.
                            _syncState.value = SyncState.Receiver.Success
                        },
                    )
                } else {
                    channel.sendAck(false)
                    _syncState.value = SyncState.Receiver.Error(
                        mapSyncError(SyncErrorCode.IMPORT_FAILED),
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "confirmImport failed", e)
                channel.sendAck(false)
                _syncState.value = SyncState.Receiver.Error(
                    mapSyncError(SyncErrorCode.IMPORT_FAILED),
                    detailOf(e),
                )
            } finally {
                pendingImportJson = null
            }
        }
    }

    fun cancelImport() {
        val channel = tcpChannel
        pendingImportJson = null
        _syncState.value = SyncState.Receiver.Error(mapSyncError(SyncErrorCode.ACK_REJECTED))
        if (channel == null) {
            stopReceiverConn()
            return
        }
        channel.sendAck(
            ok = false,
            onSendSuccess = { stopReceiverConn() },
            onSendError = { err ->
                Log.w(TAG, "Failed to send cancel ack", err)
                stopReceiverConn()
            },
        )
    }

    fun stopReceiverMode() {
        stopReceiverConn()
        receiverShareCode = null
        pendingImportJson = null
        _syncState.value = SyncState.Idle
    }

    fun stopReceiverConn() {
        udpBroadcaster?.stop()
        udpBroadcaster = null

        tcpChannel?.disconnect("closed")
        tcpChannel = null

        tcpServer?.stop()
        tcpServer = null
    }

    override fun onCleared() {
        stopSenderConn()
        stopReceiverConn()
        super.onCleared()
    }

    private suspend fun exportBadgesToJson(): String {
        val badges = BadgeRepository.getAllBadgesSnapshot()
        return Gson().toJson(badges)
    }

    private suspend fun backupLocalBadgesToCache() {
        try {
            val badges = BadgeRepository.getAllBadgesSnapshot()
            val json = Gson().toJson(badges)
            val file = File(
                getApplication<Application>().cacheDir,
                "lan_sync_backup_${System.currentTimeMillis()}.json",
            )
            file.writeText(json)
            Log.d(TAG, "Local backup saved: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to backup before import", e)
        }
    }

    private fun parseBadgeCount(jsonStr: String): Int {
        return try {
            if (jsonStr.isBlank()) return 0
            val type = object : TypeToken<List<Badge>>() {}.type
            val badges: List<Badge> = Gson().fromJson(jsonStr, type) ?: emptyList()
            badges.size
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse badge count", e)
            0
        }
    }

    private suspend fun importBadgesFromJson(jsonStr: String): Boolean {
        return try {
            if (jsonStr.isBlank()) {
                BadgeRepository.restoreBadges(emptyList())
                return true
            }
            val type = object : TypeToken<List<Badge>>() {}.type
            val badges: List<Badge> = Gson().fromJson(jsonStr, type) ?: emptyList()
            BadgeRepository.restoreBadges(badges)
            true
        } catch (e: Exception) {
            Log.e(TAG, "importBadgesFromJson failed", e)
            false
        }
    }

    companion object {
        private const val TAG = "BadgeSyncViewModel"
    }
}
