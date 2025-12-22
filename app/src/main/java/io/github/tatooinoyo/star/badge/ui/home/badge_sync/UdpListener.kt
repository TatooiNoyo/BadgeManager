package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException


class UdpListener(
    private val port: Int = 9999,
    private val keyword: String = "DISCOVER_STAR_APP",
    private val onDeviceFound: (ip: String, tcpPort: Int) -> Unit
) {
    @Volatile // 保证多线程可见性
    private var running = false
    private var socket: DatagramSocket? = null // 提升为成员变量，以便 stop() 可以访问

    fun start() {
        if (running) return
        running = true

        Thread {
            socket = DatagramSocket(port, InetAddress.getByName("0.0.0.0"))
            val buffer = ByteArray(1024)
            val packet = DatagramPacket(buffer, buffer.size)

            while (running) {
                try {
                    socket?.receive(packet)
                    Log.d("UdpListener", "Received packet from ${packet.address}")
                    val msg = String(packet.data, 0, packet.length)
                    if (msg.startsWith(keyword)) {
                        // 3. 安全解析端口 (防止格式错误导致崩溃)
                        val portPart = msg.substring(keyword.length + 1).trim()
                        val tcpPort = portPart.toIntOrNull()
                        if (tcpPort != null) {
                            val senderIp = packet.address.hostAddress
                            // 回调给上层
                            if (senderIp != null) {
                                onDeviceFound(senderIp, tcpPort)
                            }
                        } else {
                            Log.w("UdpListener", "Invalid port format: $portPart")
                        }
                    }
                } catch (e: SocketException) {
                    // 4. 这里的异常是预期的：当调用 socket.close() 时会触发
                    // 意味着我们应该退出循环了
                    Log.d("UdpListener", "Socket closed, stopping listener.")
                    break
                } catch (e: Exception) {
                    Log.e("UdpListener", "Error receiving packet", e)
                } finally {
                    // 确保清理资源
                    socket?.close()
                    running = false
                }
            }
        }.start()
    }

    fun stop() {
        running = false
        socket?.close()
    }
}