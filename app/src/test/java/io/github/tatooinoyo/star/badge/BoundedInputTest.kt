package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.utils.readBoundedBytes
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import org.junit.Assert.*
import org.junit.Test

class BoundedInputTest {
    @Test fun acceptsEmptyAndExactLimit() {
        assertArrayEquals(byteArrayOf(), ByteArrayInputStream(byteArrayOf()).readBoundedBytes(4))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)).readBoundedBytes(4))
    }

    @Test fun stopsUnboundedStreamAtLimitPlusOne() {
        var reads = 0
        val stream = object : InputStream() {
            override fun read(): Int { reads++; return 1 }
        }
        assertThrows(IOException::class.java) { stream.readBoundedBytes(100) }
        assertEquals(101, reads)
    }

    @Test fun brokenStreamCannotBusyLoop() {
        val stream = object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int = 0
        }
        assertThrows(IOException::class.java) { stream.readBoundedBytes(100) }
    }

    @Test fun maximumIntegerLimitDoesNotOverflow() {
        assertArrayEquals(byteArrayOf(1), ByteArrayInputStream(byteArrayOf(1)).readBoundedBytes(Int.MAX_VALUE))
    }
}
