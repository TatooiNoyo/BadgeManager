package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.ui.home.badge_sync.CryptoUtil
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.HandshakeManager
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.SecureChannel
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.SyncProtocol
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.TcpServer
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

class SyncTransportTest {
    @Test fun handshakeAndEncryptedPayloadRoundTrip() {
        val executor = Executors.newSingleThreadExecutor()
        val listener = ServerSocket(0)
        val remote = executor.submit<SecureChannel> {
            val socket = listener.accept()
            SecureChannel(socket, HandshakeManager.handleServerSide(socket, "123456")!!)
        }
        val socket = Socket("127.0.0.1", listener.localPort)
        var client: SecureChannel? = null
        var server: SecureChannel? = null
        try {
            client = SecureChannel(socket, HandshakeManager.handleClientSide(socket, "123456")!!)
            server = remote.get(15, TimeUnit.SECONDS)
            val received = CountDownLatch(1)
            val acknowledged = CountDownLatch(1)
            val failure = AtomicReference<Exception?>()
            val receiver = server
            server.startReceiving(onData = { data ->
                if (data != "標題與 emoji 🌟") failure.set(IllegalStateException(data))
                receiver.sendAck(true)
                received.countDown()
            }, onError = { failure.set(it); received.countDown() })
            client.startReceiving(onData = {}, onAck = { ok ->
                if (!ok) failure.set(IllegalStateException("Rejected"))
                acknowledged.countDown()
            }, onError = { failure.set(it); acknowledged.countDown() })
            client.sendData("標題與 emoji 🌟")
            assertTrue(received.await(10, TimeUnit.SECONDS))
            assertTrue(acknowledged.await(10, TimeUnit.SECONDS))
            assertNull(failure.get())
        } finally {
            client?.disconnect(notifyPeer = false)
            server?.disconnect(notifyPeer = false)
            socket.close(); listener.close(); executor.shutdownNow()
        }
    }

    @Test fun disconnectClosesBlockedWriterWithoutWaitingOnCaller() {
        val writing = CountDownLatch(1)
        val closed = CountDownLatch(1)
        val socket = object : Socket() {
            override fun getInputStream(): InputStream = ByteArrayInputStream(byteArrayOf())
            override fun getOutputStream(): OutputStream = object : OutputStream() {
                override fun write(b: Int) { writing.countDown(); closed.await(3, TimeUnit.SECONDS) }
                override fun write(b: ByteArray, off: Int, len: Int) { write(0) }
            }
            override fun close() { closed.countDown() }
        }
        val channel = SecureChannel(socket, ByteArray(32))
        try {
            channel.sendData("payload")
            assertTrue(writing.await(3, TimeUnit.SECONDS))
            val executor = Executors.newSingleThreadExecutor()
            try {
                executor.submit { channel.disconnect() }.get(1, TimeUnit.SECONDS)
                assertTrue(closed.await(2, TimeUnit.SECONDS))
            } finally { executor.shutdownNow() }
        } finally { channel.disconnect(notifyPeer = false); socket.close() }
    }

    @Test fun oversizedPayloadIsRejectedBeforeWriting() {
        val writes = AtomicInteger()
        val failed = CountDownLatch(1)
        val socket = object : Socket() {
            override fun getInputStream(): InputStream = ByteArrayInputStream(byteArrayOf())
            override fun getOutputStream(): OutputStream = object : OutputStream() {
                override fun write(b: Int) { writes.incrementAndGet() }
            }
        }
        val channel = SecureChannel(socket, ByteArray(32))
        try {
            channel.sendData("x".repeat(SyncProtocol.MAX_FRAME_BYTES), onSendError = { failed.countDown() })
            assertTrue(failed.await(3, TimeUnit.SECONDS))
            assertEquals(0, writes.get())
        } finally { channel.disconnect(notifyPeer = false) }
    }

    @Test fun slowHandshakeCannotExtendAbsoluteDeadline() {
        val socket = object : Socket() { }
        val input = object : InputStream() {
            override fun read(): Int { Thread.sleep(10); return 'x'.code }
        }
        assertThrows(SocketTimeoutException::class.java) {
            CryptoUtil.readLineFromStream(input, socket, System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(40))
        }
        socket.close()
    }

    @Test fun stopClosesPendingHandshakeAndSuppressesLateCallbacks() {
        val ready = CountDownLatch(1)
        val port = AtomicInteger()
        val channels = AtomicInteger()
        val server = TcpServer("123456")
        server.start(onServerReady = { port.set(it); ready.countDown() },
            onChannelReady = { channels.incrementAndGet(); it.disconnect(notifyPeer = false) },
            onError = { _, _ -> })
        try {
            assertTrue(ready.await(3, TimeUnit.SECONDS))
            Socket("127.0.0.1", port.get()).use { socket ->
                socket.soTimeout = 2000
                socket.getOutputStream().write('{'.code)
                server.stop()
                val disconnected = try { socket.getInputStream().read() == -1 }
                catch (e: SocketTimeoutException) { throw e }
                catch (_: java.io.IOException) { true }
                assertTrue(disconnected)
                assertEquals(0, channels.get())
            }
        } finally { server.stop() }
    }

    @Test fun onlyOneConcurrentHandshakeCanOwnTheSession() {
        val ready = CountDownLatch(1)
        val selected = CountDownLatch(1)
        val port = AtomicInteger()
        val count = AtomicInteger()
        val channel = AtomicReference<SecureChannel?>()
        val server = TcpServer("123456")
        val workers = Executors.newFixedThreadPool(2)
        val sockets = mutableListOf<Socket>()
        server.start(onServerReady = { port.set(it); ready.countDown() },
            onChannelReady = { count.incrementAndGet(); channel.set(it); selected.countDown() },
            onError = { _, _ -> })
        try {
            assertTrue(ready.await(3, TimeUnit.SECONDS))
            repeat(2) { sockets.add(Socket("127.0.0.1", port.get())) }
            val begin = CountDownLatch(1)
            val results = sockets.map { socket -> workers.submit {
                begin.await()
                runCatching { HandshakeManager.handleClientSide(socket, "123456") }
            } }
            begin.countDown()
            results.forEach { it.get(15, TimeUnit.SECONDS) }
            assertTrue(selected.await(3, TimeUnit.SECONDS))
            assertEquals(1, count.get())
        } finally {
            server.stop(); channel.get()?.disconnect(notifyPeer = false)
            sockets.forEach { it.close() }; workers.shutdownNow()
        }
    }

    @Test fun malformedFrameLengthsAreRejectedBeforePayloadRead() {
        listOf(-1, 0, 1, SyncProtocol.MAX_FRAME_BYTES + 1).forEach { length ->
            ServerSocket(0).use { listener ->
                Socket("127.0.0.1", listener.localPort).use { client ->
                    val channel = SecureChannel(listener.accept(), ByteArray(32))
                    val error = CountDownLatch(1)
                    try {
                        channel.startReceiving(onData = { fail("Invalid frame was accepted") }, onError = { error.countDown() })
                        client.getOutputStream().write(ByteBuffer.allocate(4).putInt(length).array())
                        assertTrue(error.await(3, TimeUnit.SECONDS))
                    } finally { channel.disconnect(notifyPeer = false) }
                }
            }
        }
    }
}
