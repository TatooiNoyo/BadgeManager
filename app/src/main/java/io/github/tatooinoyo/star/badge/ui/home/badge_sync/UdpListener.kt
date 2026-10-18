package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException

class UdpListener(
    private val port: Int = 9999,
    private val expectedFingerprint: String,
    private val onDeviceFound: (ip: String, tcpPort: Int) -> Unit,
    private val onError: (SyncErrorCode) -> Unit = {},
) {
    @Volatile
    private var running = false
    private val lock = Any()
    private var socket: DatagramSocket? = null
    private var started = false

    fun start() {
        synchronized(lock) {
            if (started) return
            started = true
            running = true
        }

        Thread {
            try {
                val bound = DatagramSocket(port, InetAddress.getByName("0.0.0.0"))
                synchronized(lock) {
                    if (!running) { bound.close(); return@Thread }
                    socket = bound
                }
            } catch (e: BindException) {
                Log.e("UdpListener", "Port $port already in use", e)
                if (running) onError(SyncErrorCode.PORT_IN_USE)
                running = false
                return@Thread
            } catch (e: Exception) {
                Log.e("UdpListener", "Failed to bind UDP port", e)
                if (running) onError(SyncErrorCode.NETWORK_UNAVAILABLE)
                running = false
                return@Thread
            }

            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)
            var lastDiscovery = 0L

            try {
                while (running) {
                    try {
                        packet.length = buffer.size
                        socket?.receive(packet) ?: break
                        val msg = String(packet.data, 0, packet.length)


                        val parsed = SyncProtocol.parseDiscoveryMessage(msg) ?: continue
                        if (parsed.fingerprint != expectedFingerprint) continue
                        val now = System.nanoTime()
                        if (lastDiscovery != 0L && now - lastDiscovery < 1_000_000_000L) continue
                        lastDiscovery = now

                        val senderIp = packet.address.hostAddress ?: continue
                        onDeviceFound(senderIp, parsed.tcpPort)
                    } catch (e: SocketException) {
                        if (running) {
                            Log.d("UdpListener", "Socket closed, stopping listener.")
                        }
                        break
                    } catch (e: Exception) {
                        Log.e("UdpListener", "Error receiving packet", e)
                    }
                }
            } finally {
                try {
                    socket?.close()
                } catch (_: Exception) {
                }
                socket = null
                running = false
            }
        }.start()
    }

    fun stop() {
        synchronized(lock) {
            started = true
            running = false
            runCatching { socket?.close() }
        }
    }
}
