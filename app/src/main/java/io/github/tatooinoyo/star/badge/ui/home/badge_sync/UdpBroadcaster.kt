package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets

class UdpBroadcaster(
    private val port: Int = 9999,
    private val shareCode: String,
    private val onError: (SyncErrorCode) -> Unit = {},
) {
    @Volatile
    private var running = false
    private val lock = Any()
    private var socket: DatagramSocket? = null
    private var started = false

    fun start(tcpPort: Int) {
        synchronized(lock) {
            if (started) return
            started = true
            running = true
        }

        Thread {
            try {
                val bound = DatagramSocket().apply { broadcast = true }
                synchronized(lock) {
                    if (!running) { bound.close(); return@Thread }
                    socket = bound
                }
                val message = SyncProtocol.buildDiscoveryMessage(tcpPort, shareCode)
                val buffer = message.toByteArray(StandardCharsets.UTF_8)
                val addresses = linkedSetOf(InetAddress.getByName("255.255.255.255"))
                runCatching {
                    java.net.NetworkInterface.getNetworkInterfaces().toList()
                        .filter { it.isUp && !it.isLoopback }
                        .flatMap { it.interfaceAddresses }
                        .mapNotNull { it.broadcast }
                        .forEach { addresses.add(it) }
                }
                val packet = DatagramPacket(
                    buffer,
                    buffer.size,
                    InetAddress.getByName("255.255.255.255"),
                    port,
                )

                while (running) {
                    try {
                        var sent = false
                        for (address in addresses) {
                            packet.address = address
                            try { bound.send(packet); sent = true } catch (_: Exception) { }
                        }
                        if (!sent) throw java.io.IOException("No broadcast route")
                        Thread.sleep(1500)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    } catch (e: Exception) {
                        if (running) {
                            Log.e("UdpBroadcaster", "Broadcast failed", e)
                            onError(SyncErrorCode.NETWORK_UNAVAILABLE)
                        }
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("UdpBroadcaster", "Failed to start broadcaster", e)
                if (running) {
                    onError(SyncErrorCode.NETWORK_UNAVAILABLE)
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
