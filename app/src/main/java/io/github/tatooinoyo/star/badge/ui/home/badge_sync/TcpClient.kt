package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

class TcpClient(
    private val targetIP: String,
    private val targetPort: Int = 6000,
    private val psk6: String,
) {
    private var socket: Socket? = null

    suspend fun connectAndGetChannel(): SecureChannel? =
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
                    return@withContext SecureChannel(sock, sessionKey)
                }
                sock.close()
                return@withContext null
            } catch (e: SocketTimeoutException) {
                Log.e("TcpClient", "connect/handshake timeout", e)
                socket?.close()
                return@withContext null
            } catch (e: Exception) {
                Log.e("TcpClient", "connectAndGetChannel failed", e)
                socket?.close()
                return@withContext null
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
