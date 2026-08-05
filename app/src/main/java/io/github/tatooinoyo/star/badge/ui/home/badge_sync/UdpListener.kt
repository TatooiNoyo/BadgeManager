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
    private var socket: DatagramSocket? = null

    fun start() {
        if (running) return
        running = true

        Thread {
            try {
                socket = DatagramSocket(port, InetAddress.getByName("0.0.0.0"))
            } catch (e: BindException) {
                Log.e("UdpListener", "Port $port already in use", e)
                running = false
                onError(SyncErrorCode.PORT_IN_USE)
                return@Thread
            } catch (e: Exception) {
                Log.e("UdpListener", "Failed to bind UDP port", e)
                running = false
                onError(SyncErrorCode.NETWORK_UNAVAILABLE)
                return@Thread
            }

            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            try {
                while (running) {
                    try {
                        packet.length = buffer.size
                        socket?.receive(packet) ?: break
                        val msg = String(packet.data, 0, packet.length)
                        Log.d("UdpListener", "Received packet from ${packet.address}: $msg")

                        val parsed = SyncProtocol.parseDiscoveryMessage(msg) ?: continue
                        if (parsed.fingerprint != expectedFingerprint) {
                            Log.d("UdpListener", "Ignoring discovery with mismatched fingerprint")
                            continue
                        }

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
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {
        }
    }
}
