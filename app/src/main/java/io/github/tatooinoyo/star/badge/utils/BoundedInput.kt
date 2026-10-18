package io.github.tatooinoyo.star.badge.utils

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** The limit is enforced while reading, including streams without a reported size. */
fun InputStream.readBoundedBytes(maxBytes: Int): ByteArray {
    require(maxBytes > 0)
    val output = ByteArrayOutputStream(minOf(maxBytes, 8192))
    val buffer = ByteArray(8192)
    while (true) {
        if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException()
        val count = read(buffer, 0, minOf(buffer.size.toLong(), maxBytes.toLong() - output.size() + 1).toInt())
        if (count < 0) break
        if (count == 0) throw IOException("Input stream made no progress")
        if (count > maxBytes - output.size()) throw IOException("Input exceeds $maxBytes bytes")
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
