package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.ui.home.badge_sync.CryptoUtil
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.SyncProtocol
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class SyncProtocolTest {
    @Test fun discoveryRejectsInvalidPortsAndFingerprints() {
        listOf("0", "-1", "65536", "x").forEach {
            assertNull(SyncProtocol.parseDiscoveryMessage("DISCOVER_STAR_APP:$it:AAAAAA=="))
        }
        assertNull(SyncProtocol.parseDiscoveryMessage("DISCOVER_STAR_APP:6000:short"))
        val packet = SyncProtocol.parseDiscoveryMessage("DISCOVER_STAR_APP:65535:AAAAAA==")!!
        assertEquals(65535, packet.tcpPort)
    }

    @Test fun handshakeAcceptsBoundedLineAndRejectsOversizedOrTruncatedLine() {
        assertEquals("hello", CryptoUtil.readLineFromStream(ByteArrayInputStream("hello\r\n".toByteArray())))
        assertNull(CryptoUtil.readLineFromStream(ByteArrayInputStream(byteArrayOf())))
        assertThrows(IOException::class.java) { CryptoUtil.readLineFromStream(ByteArrayInputStream("truncated".toByteArray())) }
        listOf("x", "\r").forEach { char ->
            assertThrows(IOException::class.java) {
                CryptoUtil.readLineFromStream(ByteArrayInputStream((char.repeat(SyncProtocol.MAX_HANDSHAKE_BYTES + 1) + "\n").toByteArray()))
            }
        }
    }
}
