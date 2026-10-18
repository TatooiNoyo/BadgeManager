package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.CryptoUtil
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareCrypto
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareError
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test

class BadgeShareCryptoTest {
    private val badges = listOf(Badge("a", "title", "remark", orderIndex = 1))

    @Test fun validFileRoundTripsAndWrongCodeFails() {
        val encrypted = BadgeShareCrypto.encrypt(badges, "123456")
        assertEquals(badges, BadgeShareCrypto.decrypt(encrypted, "123456").getOrThrow().badges)
        assertEquals(BadgeShareError.WrongPassword, BadgeShareCrypto.decrypt(encrypted, "654321").exceptionOrNull())
    }

    @Test fun invalidKdfCountsAreRejectedWithoutDerivingKeyOrThrowing() {
        val encrypted = BadgeShareCrypto.encrypt(badges, "123456")
        listOf(-1, 0, 1, 99_999, 300_001, Int.MAX_VALUE).forEach { count ->
            val modified = encrypted.copyOf()
            ByteBuffer.wrap(modified, 6, 4).putInt(count)
            assertEquals(BadgeShareError.InvalidFile, BadgeShareCrypto.decrypt(modified, "123456").exceptionOrNull())
        }
    }

    @Test fun tamperingAndOversizedFilesFail() {
        val encrypted = BadgeShareCrypto.encrypt(badges, "123456")
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        assertTrue(BadgeShareCrypto.decrypt(encrypted, "123456").isFailure)
        assertFalse(BadgeShareCrypto.looksLikeEncryptedShare(ByteArray(BadgeShareCrypto.MAX_FILE_BYTES + 1)))
    }

    @Test fun authenticatedButInvalidBadgeDataIsRejected() {
        val template = BadgeShareCrypto.encrypt(badges, "123456")
        val salt = template.copyOfRange(10, 26)
        val key = CryptoUtil.derivePskKeyFromCode("123456", salt)
        val json = """{"format":"badge-share","formatVersion":1,"badges":[{"id":"a","title":null,"remark":"","orderIndex":0}]}"""
        val malformed = template.copyOfRange(0, 26) + CryptoUtil.aesGcmEncrypt(key, json.toByteArray())
        assertEquals(BadgeShareError.InvalidFile, BadgeShareCrypto.decrypt(malformed, "123456").exceptionOrNull())
    }
}
