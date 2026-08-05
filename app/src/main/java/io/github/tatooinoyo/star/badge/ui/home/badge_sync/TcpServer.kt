package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import java.net.ServerSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicInteger

class TcpServer(
    private val psk6: String,
) {
    companion object {
        const val ERROR_HANDSHAKE_FAILED = "HANDSHAKE_FAILED"
        const val ERROR_LISTENER_DISCONNECTED = "LISTENER_DISCONNECTED"
        const val ERROR_TOO_MANY_HANDSHAKE_FAILURES = "TOO_MANY_HANDSHAKE_FAILURES"
    }

    @Volatile
    private var running = false
    private var serverSocket: ServerSocket? = null
    private val handshakeFailures = AtomicInteger(0)

    val listeningPort: Int
        get() = serverSocket?.localPort ?: -1

    fun start(
        onServerReady: (port: Int) -> Unit,
        onChannelReady: (SecureChannel) -> Unit,
        onError: (code: String, cause: Throwable?) -> Unit,
    ) {
        if (running) return
        running = true
        handshakeFailures.set(0)

        Thread {
            try {
                serverSocket = ServerSocket(0)
            } catch (e: Exception) {
                onError(SyncErrorCode.NETWORK_UNAVAILABLE.name, e)
                running = false
                return@Thread
            }
            onServerReady(listeningPort)
            val server = serverSocket!!
            try {
                while (running) {
                    val clientSocket = server.accept()
                    Log.d("TcpServer", "Client connected: $clientSocket")
                    clientSocket.soTimeout = SyncProtocol.HANDSHAKE_TIMEOUT_MS

                    Thread {
                        try {
                            val sessionKey = HandshakeManager.handleServerSide(clientSocket, psk6)
                            if (sessionKey != null) {
                                clientSocket.soTimeout = 0
                                handshakeFailures.set(0)
                                running = false
                                try {
                                    server.close()
                                } catch (_: Exception) {
                                }
                                serverSocket = null
                                onChannelReady(SecureChannel(clientSocket, sessionKey))
                            } else {
                                clientSocket.close()
                                val failures = handshakeFailures.incrementAndGet()
                                val cause = IllegalStateException("Handshake returned null session key")
                                if (failures >= SyncProtocol.MAX_HANDSHAKE_FAILURES) {
                                    running = false
                                    try {
                                        server.close()
                                    } catch (_: Exception) {
                                    }
                                    serverSocket = null
                                    onError(ERROR_TOO_MANY_HANDSHAKE_FAILURES, cause)
                                } else {
                                    onError(ERROR_HANDSHAKE_FAILED, cause)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TcpServer", "Handshake error", e)
                            try {
                                clientSocket.close()
                            } catch (_: Exception) {
                            }
                            val failures = handshakeFailures.incrementAndGet()
                            if (failures >= SyncProtocol.MAX_HANDSHAKE_FAILURES) {
                                running = false
                                try {
                                    server.close()
                                } catch (_: Exception) {
                                }
                                serverSocket = null
                                onError(ERROR_TOO_MANY_HANDSHAKE_FAILURES, e)
                            } else {
                                onError(ERROR_HANDSHAKE_FAILED, e)
                            }
                        }
                    }.start()
                }
                try {
                    server.close()
                } catch (_: Exception) {
                }
            } catch (e: SocketException) {
                if (running) {
                    Log.e("TcpServer", "Accept failed", e)
                    onError(ERROR_LISTENER_DISCONNECTED, e)
                } else {
                    Log.d("TcpServer", "Server stopped normally")
                }
            } catch (e: Exception) {
                Log.e("TcpServer", "Accept error", e)
                if (running) {
                    onError(ERROR_LISTENER_DISCONNECTED, e)
                }
            } finally {
                running = false
            }
        }.start()
    }

    fun stop() {
        running = false
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
    }
}
