package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

sealed interface ConnectResult {
    data class Ok(val channel: SecureChannel) : ConnectResult
    data class Fail(val code: SyncErrorCode, val cause: Throwable?) : ConnectResult
}

class TcpClient(
    private val targetIP: String,
    private val targetPort: Int = 6000,
    private val psk6: String,
) {
    private var socket: Socket? = null

    suspend fun connectAndGetChannel(): ConnectResult =
        withContext(Dispatchers.IO) {
            try {
                val sock = Socket()
                socket = sock
                sock.connect(
                    InetSocketAddress(targetIP, targetPort),
                    SyncProtocol.CONNECT_TIMEOUT_MS,
                )
                sock.soTimeout = SyncProtocol.HANDSHAKE_TIMEOUT_MS

                val sessionKey = HandshakeManager.handleClientSide(sock, psk6)
                if (sessionKey != null) {
                    sock.soTimeout = 0
                    return@withContext ConnectResult.Ok(SecureChannel(sock, sessionKey))
                }
                sock.close()
                return@withContext ConnectResult.Fail(
                    SyncErrorCode.HANDSHAKE_FAILED,
                    IllegalStateException("Handshake returned null session key"),
                )
            } catch (e: SocketTimeoutException) {
                Log.e("TcpClient", "connect/handshake timeout", e)
                socket?.close()
                return@withContext ConnectResult.Fail(SyncErrorCode.CONNECT_TIMEOUT, e)
            } catch (e: Exception) {
                Log.e("TcpClient", "connectAndGetChannel failed", e)
                socket?.close()
                return@withContext ConnectResult.Fail(SyncErrorCode.TRANSFER_INTERRUPTED, e)
            }
        }

    fun close() {
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }
}
