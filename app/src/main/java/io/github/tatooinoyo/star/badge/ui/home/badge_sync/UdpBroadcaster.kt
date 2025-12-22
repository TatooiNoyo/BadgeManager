package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets


class UdpBroadcaster(
    private val port: Int = 9999,
    private val message: String = "DISCOVER_STAR_APP"
) {
    private var running = false

    fun start(tcpPort: String) {
        if (running) return
        running = true
        Thread {
            val socket = DatagramSocket()
            socket.broadcast = true
            val bufferStr = "$message:$tcpPort"
            val buffer = bufferStr.toByteArray(StandardCharsets.UTF_8)
            val packet = DatagramPacket(
                buffer,
                buffer.size,
                InetAddress.getByName("255.255.255.255"),
                port
            )

            while (running) {
                socket.send(packet)
                Thread.sleep(1500)
            }

            socket.close()
        }.start()
    }

    fun stop() {
        running = false
    }
}



