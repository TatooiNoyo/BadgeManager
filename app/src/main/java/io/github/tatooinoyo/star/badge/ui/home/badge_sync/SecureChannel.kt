package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class SecureChannel(private val socket: Socket, private val sessionKey: ByteArray) {
    val remoteAddress: String get() = socket.inetAddress?.hostAddress ?: "?"
    private val input = socket.getInputStream()
    private val output = socket.getOutputStream()
    private val receiving = AtomicBoolean(false)
    private val closing = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val recvExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "badge-receive").apply { isDaemon = true }
    }
    private val sendExecutor = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS, ArrayBlockingQueue(4),
        { task -> Thread(task, "badge-send").apply { isDaemon = true } },
    )

    fun startReceiving(
        onData: (String) -> Unit,
        onAck: (Boolean) -> Unit = {},
        onPayloadReceived: () -> Unit = {},
        onDisconnect: (String?) -> Unit = {},
        onFirstFrame: (() -> Unit)? = null,
        onError: (Exception) -> Unit = {},
    ) {
        if (closing.get() || !receiving.compareAndSet(false, true)) return
        try {
            recvExecutor.execute {
                var first = true
                val seen = HashSet<String>()
                try {
                    while (!closing.get()) {
                        val header = ByteArray(4)
                        // Includes user confirmation time; the ViewModel applies shorter phase deadlines.
                        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(
                            (SyncProtocol.CONFIRM_TIMEOUT_MS + SyncProtocol.TRANSFER_TIMEOUT_MS).toLong(),
                        )
                        readFully(input, header, deadline)
                        val length = ByteBuffer.wrap(header).int
                        if (length !in (GCM_OVERHEAD_BYTES + 1)..SyncProtocol.MAX_FRAME_BYTES) throw IOException("Bad frame length")
                        val payload = ByteArray(length)
                        readFully(input, payload, deadline)
                        val plaintext = String(CryptoUtil.aesGcmDecrypt(sessionKey, payload, header), Charsets.UTF_8)
                        BadgeImportValidator.requireShallowJson(plaintext)
                        val obj = JSONObject(plaintext)
                        SyncProtocol.requireVersion(obj)
                        if (first) { first = false; onFirstFrame?.invoke() }
                        val type = obj.getString("type")
                        if (!seen.add(type)) throw IOException("Repeated message: $type")
                        when (type) {
                            "data" -> {
                                val data = obj.getString("data")
                                if (data.length > BadgeImportValidator.MAX_JSON_BYTES || data.toByteArray(Charsets.UTF_8).size > BadgeImportValidator.MAX_JSON_BYTES) throw IOException("Payload is too large")
                                onData(data)
                            }
                            "ack" -> onAck(obj.getBoolean("ok"))
                            "received" -> onPayloadReceived()
                            "disconnect" -> {
                                onDisconnect(obj.optString("reason"))
                                disconnect(notifyPeer = false)
                            }
                            else -> throw IOException("Unknown message type")
                        }
                    }
                } catch (e: Exception) {
                    if (!closing.get()) onError(e)
                } finally {
                    closeResources()
                }
            }
        } catch (e: RejectedExecutionException) {
            if (!closing.get()) onError(e)
        }
    }

    fun sendData(data: String, onSendSuccess: () -> Unit = {}, onSendError: (Exception) -> Unit = {}) {
        if (data.length > BadgeImportValidator.MAX_JSON_BYTES) {
            onSendError(IOException("Payload is too large"))
            return
        }
        sendEnvelope(SyncProtocol.buildEnvelope("data") { put("data", data) }, onSendSuccess, onSendError)
    }

    fun sendAck(ok: Boolean, onSendSuccess: () -> Unit = {}, onSendError: (Exception) -> Unit = {}) =
        sendEnvelope(SyncProtocol.buildEnvelope("ack") { put("ok", ok) }, onSendSuccess, onSendError)

    fun sendPayloadReceived(onSendSuccess: () -> Unit = {}, onSendError: (Exception) -> Unit = {}) =
        sendEnvelope(SyncProtocol.buildEnvelope("received"), onSendSuccess, onSendError)

    private fun sendEnvelope(envelope: JSONObject, onSendSuccess: () -> Unit, onSendError: (Exception) -> Unit) {
        if (closing.get()) { onSendError(IOException("Channel closed")); return }
        try {
            sendExecutor.execute {
                val deadline = deadlines.schedule({ closeResources() }, SyncProtocol.TRANSFER_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                try {
                    if (closing.get()) throw IOException("Channel closed")
                    writeFrame(envelope)
                    onSendSuccess()
                } catch (e: Exception) {
                    onSendError(e)
                    closeResources()
                } finally {
                    deadline.cancel(false)
                }
            }
        } catch (e: RejectedExecutionException) {
            onSendError(e)
        }
    }

    /** Never wait for a writer or its lock on the caller (which may be the UI thread). */
    fun disconnect(reason: String = "user_exit", notifyPeer: Boolean = true) {
        if (!closing.compareAndSet(false, true)) return
        if (!notifyPeer) { closeResources(); return }
        val deadline = deadlines.schedule({ closeResources() }, 200, TimeUnit.MILLISECONDS)
        try {
            sendExecutor.execute {
                try { writeFrame(SyncProtocol.buildEnvelope("disconnect") { put("reason", reason) }) }
                catch (_: Exception) { }
                finally { deadline.cancel(false); closeResources() }
            }
        } catch (_: RejectedExecutionException) {
            deadline.cancel(false)
            closeResources()
        }
    }

    private fun closeResources() {
        closing.set(true)
        if (!closed.compareAndSet(false, true)) return
        // Closing the socket unblocks reads and writes; interrupt alone does not.
        runCatching { socket.close() }
        recvExecutor.shutdownNow()
        sendExecutor.shutdownNow()
    }

    private fun writeFrame(envelope: JSONObject) {
        val plaintext = envelope.toString().toByteArray(Charsets.UTF_8)
        val length = plaintext.size.toLong() + GCM_OVERHEAD_BYTES
        if (length > SyncProtocol.MAX_FRAME_BYTES) throw IOException("Frame is too large")
        val header = ByteBuffer.allocate(4).putInt(length.toInt()).array()
        val payload = CryptoUtil.aesGcmEncrypt(sessionKey, plaintext, header)
        output.write(header)
        output.write(payload)
        output.flush()
    }

    private fun readFully(input: InputStream, buffer: ByteArray, deadline: Long) {
        var offset = 0
        while (offset < buffer.size) {
            val remaining = deadline - System.nanoTime()
            if (remaining <= 0) throw java.net.SocketTimeoutException("Frame deadline exceeded")
            socket.soTimeout = TimeUnit.NANOSECONDS.toMillis(remaining).coerceIn(1, Int.MAX_VALUE.toLong()).toInt()
            val count = input.read(buffer, offset, buffer.size - offset)
            if (count == -1) throw IOException("Stream closed")
            offset += count
        }
    }

    companion object {
        private const val GCM_OVERHEAD_BYTES = 28
        private val deadlines = ScheduledThreadPoolExecutor(1) { task ->
            Thread(task, "badge-io-deadlines").apply { isDaemon = true }
        }.apply { removeOnCancelPolicy = true }
    }
}
