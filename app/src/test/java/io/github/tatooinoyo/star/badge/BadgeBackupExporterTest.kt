package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator
import io.github.tatooinoyo.star.badge.utils.export.BadgeBackupExporter
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.junit.Assert.*
import org.junit.Test

class BadgeBackupExporterTest {
    private val badges = listOf(
        Badge(title = "徽章 🌟", remark = "<remark>", orderIndex = 1, tags = listOf("标签"))
    )

    @Test fun successfulExportRoundTripsAndClosesStream() {
        var closed = false
        var flushed = false
        val stream = object : ByteArrayOutputStream() {
            override fun flush() { flushed = true; super.flush() }
            override fun close() { closed = true; super.close() }
        }
        BadgeBackupExporter.write(badges) { stream }
        assertTrue(flushed)
        assertTrue(closed)
        assertEquals(badges, BadgeImportValidator.parse(stream.toString("UTF-8")))
    }

    @Test fun missingOutputStreamIsFailure() {
        assertThrows(IOException::class.java) { BadgeBackupExporter.write(badges) { null } }
    }

    @Test fun writeFailureIsPropagatedAndClosesStream() {
        var closed = false
        val failure = IOException("write failed")
        val stream = object : OutputStream() {
            override fun write(value: Int) { throw failure }
            override fun close() { closed = true }
        }
        assertSame(failure, assertThrows(IOException::class.java) {
            BadgeBackupExporter.write(badges) { stream }
        })
        assertTrue(closed)
    }

    @Test fun flushFailureIsPropagatedAndClosesStream() {
        var closed = false
        val failure = IOException("flush failed")
        val stream = object : ByteArrayOutputStream() {
            override fun flush() { throw failure }
            override fun close() { closed = true }
        }
        assertSame(failure, assertThrows(IOException::class.java) {
            BadgeBackupExporter.write(badges) { stream }
        })
        assertTrue(closed)
    }

    @Test fun closeFailureIsNotReportedAsSuccess() {
        val failure = IOException("close failed")
        val stream = object : ByteArrayOutputStream() {
            override fun close() { throw failure }
        }
        assertSame(failure, assertThrows(IOException::class.java) {
            BadgeBackupExporter.write(badges) { stream }
        })
    }
}
