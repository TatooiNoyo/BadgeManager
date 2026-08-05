package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SecureChannel(
    private val socket: Socket,
    private val sessionKey: ByteArray,
) {
    val remoteAddress: String
        get() = socket.inetAddress?.hostAddress ?: "?"
    private val input: InputStream = socket.getInputStream()
    private val output: OutputStream = socket.getOutputStream()
    private val recvExecutor = Executors.newSingleThreadExecutor()
    private val sendExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var running = true

    fun startReceiving(
        onData: (String) -> Unit,
        onAck: (Boolean) -> Unit = {},
        onPayloadReceived: () -> Unit = {},
        onDisconnect: (String?) -> Unit = {},
        onFirstFrame: (() -> Unit)? = null,
        onError: (Exception) -> Unit = {},
    ) {
        recvExecutor.submit {
            var firstFrame = true
            try {
                val headerBuffer = ByteArray(4)
                while (running) {
                    readFully(input, headerBuffer, 4)
                    val length = ByteBuffer.wrap(headerBuffer).int
                    if (length !in 1..SyncProtocol.MAX_FRAME_BYTES) {
                        throw IOException("Bad frame length: $length")
                    }

                    val payload = ByteArray(length)
                    readFully(input, payload, length)
                    val plaintext = CryptoUtil.aesGcmDecrypt(sessionKey, payload, headerBuffer)
                    val msg = String(plaintext, Charsets.UTF_8)

                    val obj = JSONObject(msg)
                    SyncProtocol.requireVersion(obj)

                    if (firstFrame) {
                        firstFrame = false
                        onFirstFrame?.invoke()
                    }

                    when (obj.getString("type")) {
                        "data" -> onData(obj.getString("data"))
                        "ack" -> onAck(obj.getBoolean("ok"))
                        "received" -> onPayloadReceived()
                        "disconnect" -> {
                            Log.d("SecureChannel", "Peer disconnect: ${obj.optString("reason")}")
                            onDisconnect(obj.optString("reason"))
                            disconnect(notifyPeer = false)
                        }
                        else -> Log.w("SecureChannel", "Unknown message type: ${obj.optString("type")}")
                    }
                }
            } catch (e: SyncProtocolException) {
                if (running) {
                    Log.w("SecureChannel", "Protocol error: ${e.code}", e)
                    onError(e)
                }
                disconnect(notifyPeer = false)
            } catch (e: Exception) {
                if (running) {
                    Log.w("SecureChannel", "Receive error", e)
                    onError(e)
                }
                disconnect(notifyPeer = false)
            }
        }
    }

    fun sendData(data: String, onSendSuccess: () -> Unit = {}, onSendError: (Exception) -> Unit = {}) {
        val envelope = SyncProtocol.buildEnvelope("data") { put("data", data) }
        sendEnvelope(envelope, onSendSuccess, onSendError)
    }

    fun sendAck(ok: Boolean, onSendSuccess: () -> Unit = {}, onSendError: (Exception) -> Unit = {}) {
        val envelope = SyncProtocol.buildEnvelope("ack") { put("ok", ok) }
        sendEnvelope(envelope, onSendSuccess, onSendError)
    }

    fun sendPayloadReceived(onSendSuccess: () -> Unit = {}, onSendError: (Exception) -> Unit = {}) {
        val envelope = SyncProtocol.buildEnvelope("received")
        sendEnvelope(envelope, onSendSuccess, onSendError)
    }

    private fun sendEnvelope(
        envelope: JSONObject,
        onSendSuccess: () -> Unit = {},
        onSendError: (Exception) -> Unit = {},
    ) {
        if (!running) {
            onSendError(IOException("Channel already closed"))
            return
        }

        sendExecutor.submit {
            try {
                val frame = buildEncryptedFrame(envelope)
                synchronized(output) {
                    if (!socket.isClosed && running) {
                        output.write(frame.lengthHeader)
                        output.write(frame.payload)
                        output.flush()
                        onSendSuccess()
                    } else {
                        onSendError(IOException("Socket closed before send"))
                    }
                }
            } catch (e: Exception) {
                Log.w("SecureChannel", "Send failed", e)
                onSendError(e)
            }
        }
    }

    fun disconnect(reason: String = "user_exit", notifyPeer: Boolean = true) {
        synchronized(this) {
            if (!running) return
            running = false
        }

        fun closeResources() {
            try {
                recvExecutor.shutdownNow()
                sendExecutor.shutdown()
                try {
                    sendExecutor.awaitTermination(300, TimeUnit.MILLISECONDS)
                } catch (_: Exception) {
                }
                sendExecutor.shutdownNow()
                if (!socket.isClosed) {
                    socket.shutdownOutput()
                    socket.close()
                }
            } catch (e: Exception) {
                Log.w("SecureChannel", "closeResources failed", e)
            }
        }

        if (notifyPeer && !socket.isClosed) {
            try {
                val envelope = SyncProtocol.buildEnvelope("disconnect") { put("reason", reason) }
                val frame = buildEncryptedFrame(envelope)
                synchronized(output) {
                    if (!socket.isClosed) {
                        output.write(frame.lengthHeader)
                        output.write(frame.payload)
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                Log.w("SecureChannel", "Failed to notify peer on disconnect", e)
            }
        }
        closeResources()
    }

    /**
     * Wire frame: [4-byte ciphertext length][IV||ciphertext||tag].
     * Length AAD must match the on-wire length field (ciphertext blob size, not plaintext).
     */
    private fun buildEncryptedFrame(envelope: JSONObject): EncryptedFrame {
        val plaintext = envelope.toString().toByteArray(Charsets.UTF_8)
        val cipherLen = plaintext.size + GCM_OVERHEAD_BYTES
        val lengthHeader = ByteBuffer.allocate(4).putInt(cipherLen).array()
        val payload = CryptoUtil.aesGcmEncrypt(sessionKey, plaintext, lengthHeader)
        if (payload.size != cipherLen) {
            throw IOException("Encrypted payload size mismatch: expected $cipherLen, got ${payload.size}")
        }
        return EncryptedFrame(lengthHeader, payload)
    }

    private fun readFully(input: InputStream, buffer: ByteArray, length: Int) {
        var offset = 0
        while (offset < length) {
            val read = input.read(buffer, offset, length - offset)
            if (read == -1) throw IOException("Stream closed")
            offset += read
        }
    }

    private data class EncryptedFrame(val lengthHeader: ByteArray, val payload: ByteArray)

    companion object {
        /** AES-GCM: 12-byte IV + 16-byte auth tag. */
        private const val GCM_OVERHEAD_BYTES = 12 + 16
    }
}
