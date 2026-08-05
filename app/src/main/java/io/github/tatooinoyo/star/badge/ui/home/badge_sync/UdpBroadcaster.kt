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
    private var socket: DatagramSocket? = null

    fun start(tcpPort: Int) {
        if (running) return
        running = true

        Thread {
            try {
                socket = DatagramSocket().apply { broadcast = true }
                val message = SyncProtocol.buildDiscoveryMessage(tcpPort, shareCode)
                val buffer = message.toByteArray(StandardCharsets.UTF_8)
                val packet = DatagramPacket(
                    buffer,
                    buffer.size,
                    InetAddress.getByName("255.255.255.255"),
                    port,
                )

                while (running) {
                    try {
                        socket?.send(packet)
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
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {
        }
    }
}
