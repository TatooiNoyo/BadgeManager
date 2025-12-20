package io.github.tatooinoyo.star.badge.service.utils

import android.util.Log
import java.net.ServerSocket
import java.net.SocketException


class TcpServer(
    private val psk6: String,
) {
    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null

    // 新增属性：获取实际监听的端口
    // 如果 serverSocket 还没启动，返回 -1
    val listeningPort: Int
        get() = serverSocket?.localPort ?: -1


    fun start(
        onServerReady: (port: Int) -> Unit,
        onChannelReady: (SecureChannel) -> Unit,
        onError: (String) -> Unit
    ) {
        if (running) return
        running = true
        Thread {
            try {
                serverSocket = ServerSocket(0)
            } catch (e: Exception) {
                onError(e.stackTraceToString())
                return@Thread
            }
            onServerReady(listeningPort)
            val server = serverSocket!!
            try {
                while (running) {
                    val clientSocket = server.accept()
                    Log.d("TcpServer", "Client connected: $clientSocket")
                    // 启动 handshake 管理器在单独线程处理
                    Thread {
                        try {
                            val sessionKey = HandshakeManager.handleServerSide(clientSocket, psk6)
                            if (sessionKey != null) {
                                // 2. 创建 Channel
                                val channel = SecureChannel(clientSocket, sessionKey)
                                onChannelReady(channel)
                            } else {
                                onError("握手失败!")
                                clientSocket.close()
                            }
                        } catch (e: Exception) {
                            onError(e.stackTraceToString())
                            Log.e("TcpServer", e.stackTraceToString())
                            clientSocket.close()
                        }
                    }.start()
                }
                server.close()
            } catch (e: SocketException) {
                // 4. 优雅退出：当调用 stop() 关闭 socket 时，accept 会抛出此异常
                if (running) {
                    // 如果 running 还是 true，说明是异常关闭
                    Log.e("TcpServer", "Accept failed", e)
                    onError("监听服务异常断开")
                } else {
                    // 如果 running 是 false，说明是用户主动 stop()，属于正常退出流程
                    Log.d("TcpServer", "Server stopped normally")
                }
            } catch (e: Exception) {
                Log.e("TcpServer", "Accept error", e)
            }
        }.start()
    }


    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}