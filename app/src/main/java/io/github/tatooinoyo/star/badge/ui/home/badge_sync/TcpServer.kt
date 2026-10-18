package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TcpServer(private val psk6: String) {
    companion object {
        const val ERROR_HANDSHAKE_FAILED = "HANDSHAKE_FAILED"
        const val ERROR_LISTENER_DISCONNECTED = "LISTENER_DISCONNECTED"
    }

    private val lock = Any()
    private var running = false
    private var started = false
    private var serverSocket: ServerSocket? = null
    private val clients = HashSet<Socket>()
    // Bound both worker count and per-peer failures; a bad peer cannot lock out everyone.
    private val failures = LinkedHashMap<String, Pair<Int, Long>>()
    private val workers = ThreadPoolExecutor(
        SyncProtocol.MAX_PENDING_HANDSHAKES, SyncProtocol.MAX_PENDING_HANDSHAKES,
        0L, TimeUnit.MILLISECONDS, SynchronousQueue(),
        { task -> Thread(task, "badge-handshake").apply { isDaemon = true } },
    )

    fun start(
        onServerReady: (Int) -> Unit,
        onChannelReady: (SecureChannel) -> Unit,
        onError: (String, Throwable?) -> Unit,
    ) {
        synchronized(lock) {
            if (started) return
            started = true
            running = true
        }
        Thread({
            try {
                val server = ServerSocket(0)
                synchronized(lock) {
                    if (!running) { server.close(); return@Thread }
                    serverSocket = server
                    onServerReady(server.localPort)
                }
                while (true) {
                    val client = server.accept()
                    val address = client.inetAddress.hostAddress.orEmpty()
                    val accepted = synchronized(lock) {
                        val failure = failures[address]
                        val blocked = failure != null && failure.first >= SyncProtocol.MAX_HANDSHAKE_FAILURES &&
                            System.nanoTime() - failure.second < TimeUnit.SECONDS.toNanos(30)
                        if (!running || blocked || clients.size >= SyncProtocol.MAX_PENDING_HANDSHAKES) false
                        else clients.add(client)
                    }
                    if (!accepted) { client.close(); continue }
                    try {
                        workers.execute {
                            var transferred = false
                            try {
                                client.soTimeout = SyncProtocol.HANDSHAKE_TIMEOUT_MS
                                val key = HandshakeManager.handleServerSide(client, psk6)
                                    ?: throw IllegalArgumentException("Handshake rejected")
                                synchronized(lock) {
                                    if (running) {
                                        val channel = SecureChannel(client, key)
                                        running = false
                                        clients.remove(client)
                                        server.close()
                                        clients.forEach { runCatching { it.close() } }
                                        clients.clear()
                                        transferred = true
                                        onChannelReady(channel)
                                        workers.shutdown()
                                    }
                                }
                            } catch (e: Exception) {
                                synchronized(lock) {
                                    if (running) {
                                        val previous = failures[address]
                                        val count = if (previous != null && System.nanoTime() - previous.second < TimeUnit.SECONDS.toNanos(30)) previous.first + 1 else 1
                                        failures[address] = count to System.nanoTime()
                                        if (failures.size > 128) failures.remove(failures.keys.first())
                                        onError(ERROR_HANDSHAKE_FAILED, e)
                                    }
                                }
                            } finally {
                                synchronized(lock) { clients.remove(client) }
                                if (!transferred) runCatching { client.close() }
                            }
                        }
                    } catch (_: RejectedExecutionException) {
                        synchronized(lock) { clients.remove(client) }
                        client.close()
                    }
                }
            } catch (e: Exception) {
                synchronized(lock) {
                    if (running) onError(ERROR_LISTENER_DISCONNECTED, e)
                }
            } finally {
                stop()
            }
        }, "badge-tcp-listener").apply { isDaemon = true }.start()
    }

    fun stop() {
        synchronized(lock) {
            running = false
            started = true
            runCatching { serverSocket?.close() }
            serverSocket = null
            clients.forEach { runCatching { it.close() } }
            clients.clear()
            workers.shutdownNow()
        }
    }
}
