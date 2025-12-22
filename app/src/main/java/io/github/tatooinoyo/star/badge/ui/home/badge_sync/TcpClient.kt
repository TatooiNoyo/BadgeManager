package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

class TcpClient(
    private val targetIP: String,
    private val targetPort: Int = 6000,
    private val psk6: String,
) {
    private var socket: Socket? = null

    /**
     * 建立 TCP 连接并执行握手（协程版，非阻塞）
     * 必须在子线程/协程中调用，禁止主线程执行
     */
    suspend fun connectAndGetChannel(): SecureChannel? =
        withContext(Dispatchers.IO) {
            try {
                socket = Socket(targetIP, targetPort)

                // 1. 在内部执行握手
                val sessionKey = HandshakeManager.handleClientSide(socket!!, psk6)

                if (sessionKey != null) {
                    // 2. 握手成功，直接创建并返回 SecureChannel
                    return@withContext SecureChannel(socket!!, sessionKey)
                } else {
                    socket?.close()
                    return@withContext null
                }
            } catch (e: Exception) {
                Log.e("TcpClient", "connectAndGetChannel: ${e.stackTraceToString()}")
                socket?.close()
                return@withContext null
            }
        }


    /**
     * 关闭 Socket 连接（线程安全，兜底释放资源）
     */

    fun close() {
        socket?.close()
    }
}