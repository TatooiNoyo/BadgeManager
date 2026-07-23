package io.github.tatooinoyo.star.badge.utils.export

import com.google.gson.Gson
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.CryptoUtil
import java.nio.ByteBuffer
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object BadgeShareCrypto {

    private val gson = Gson()

    private val MAGIC = byteArrayOf('B'.code.toByte(), 'M'.code.toByte(), 'E'.code.toByte(), 'N'.code.toByte())
    private const val VERSION: Byte = 1
    private const val KDF_ID: Byte = 1
    const val PBKDF2_ITERATIONS = 100_000
    private const val SALT_SIZE = 16
    private const val KEY_BITS = 256
    private const val HEADER_SIZE = 4 + 1 + 1 + 4 + SALT_SIZE

    fun generateShareCode(): String = (100_000..999_999).random().toString()

    /** True when bytes look like a BadgeManager encrypted share (BMEN header). */
    fun looksLikeEncryptedShare(data: ByteArray): Boolean {
        if (data.size < HEADER_SIZE + 12 + 16) return false
        return data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }

    fun encrypt(badges: List<Badge>, code: String): ByteArray {
        require(code.length == 6 && code.all { it.isDigit() }) { "Share code must be 6 digits" }
        val envelope = BadgeShareEnvelope(badges = badges)
        val plaintext = gson.toJson(envelope).toByteArray(Charsets.UTF_8)

        val salt = ByteArray(SALT_SIZE).also { java.security.SecureRandom().nextBytes(it) }
        val key = deriveKey(code, salt, PBKDF2_ITERATIONS)
        val ivAndCipher = CryptoUtil.aesGcmEncrypt(key, plaintext)

        return buildHeader(salt, PBKDF2_ITERATIONS) + ivAndCipher
    }

    fun decrypt(data: ByteArray, code: String): Result<BadgeShareEnvelope> {
        if (!looksLikeEncryptedShare(data)) {
            return Result.failure(BadgeShareError.InvalidFile)
        }

        var offset = MAGIC.size
        val version = data[offset++]
        val kdfId = data[offset++]
        if (version != VERSION || kdfId != KDF_ID) {
            return Result.failure(BadgeShareError.UnsupportedVersion)
        }

        val iterations = ByteBuffer.wrap(data, offset, 4).getInt()
        offset += 4
        val salt = data.copyOfRange(offset, offset + SALT_SIZE)
        offset += SALT_SIZE
        val ivAndCipher = data.copyOfRange(offset, data.size)

        val key = deriveKey(code, salt, iterations)
        val plaintext = try {
            CryptoUtil.aesGcmDecrypt(key, ivAndCipher)
        } catch (_: Exception) {
            return Result.failure(BadgeShareError.WrongPassword)
        }

        return try {
            val envelope = gson.fromJson(String(plaintext, Charsets.UTF_8), BadgeShareEnvelope::class.java)
            if (envelope.format != BadgeShareEnvelope.FORMAT_NAME) {
                Result.failure(BadgeShareError.InvalidFile)
            } else if (envelope.badges.isEmpty()) {
                Result.failure(BadgeShareError.EmptyPayload)
            } else {
                Result.success(envelope)
            }
        } catch (_: Exception) {
            Result.failure(BadgeShareError.InvalidFile)
        }
    }

    private fun buildHeader(salt: ByteArray, iterations: Int): ByteArray {
        val header = ByteArray(HEADER_SIZE)
        var offset = 0
        System.arraycopy(MAGIC, 0, header, offset, MAGIC.size)
        offset += MAGIC.size
        header[offset++] = VERSION
        header[offset++] = KDF_ID
        ByteBuffer.wrap(header, offset, 4).putInt(iterations)
        offset += 4
        System.arraycopy(salt, 0, header, offset, SALT_SIZE)
        return header
    }

    private fun deriveKey(code: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(code.toCharArray(), salt, iterations, KEY_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }
}
