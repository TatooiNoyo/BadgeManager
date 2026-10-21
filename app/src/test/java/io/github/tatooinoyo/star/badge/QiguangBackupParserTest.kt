package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.utils.QiguangBackupParser
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.utils.export.BadgeBackupExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class QiguangBackupParserTest {
    private val validLink: String by lazy {
        val payload = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("s=token123&sk=SKU-123".toByteArray(Charsets.UTF_8))
        "https://sky.example.test/badge?s=$payload"
    }

    @Test
    fun parsesQiguangJsonAndMapsNameNoteAndCategory() {
        val json = """
            {
              "app":"sky-badge",
              "version":1,
              "categories":[{"id":"cat-a","label":"Event"}],
              "badges":[{
                "id":"source-1",
                "name":"Light Child",
                "note":"A note",
                "categoryId":"cat-a",
                "profile":{"skyMeta":{"url":"$validLink","sku":"SKU-123"}}
              }]
            }
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val parsed = QiguangBackupParser.parse(json)

        assertEquals(1, parsed.badges.size)
        assertEquals("Light Child", parsed.badges.single().title)
        assertEquals("A note", parsed.badges.single().remark)
        assertEquals(validLink, parsed.badges.single().link)
        assertEquals(BadgeChannel.SKY, parsed.badges.single().channel)
        assertEquals(listOf("Event"), parsed.badges.single().tags)
        assertEquals(0, parsed.skippedCount)
        assertFalse(parsed.encryptedShare)
    }

    @Test
    fun skipsRecordsWithoutUsableBadgeLinks() {
        val json = """
            {"badges":[
              {"name":"valid","profile":{"skyMeta":{"url":"$validLink"}}},
              {"name":"missing link","profile":{"skyMeta":{"url":"https://example.test/"}}}
            ]}
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val parsed = QiguangBackupParser.parse(json)

        assertEquals(1, parsed.badges.size)
        assertEquals(1, parsed.skippedCount)
        assertThrows(IllegalArgumentException::class.java) {
            QiguangBackupParser.parse("{\"badges\":[{\"name\":\"bad\"}]}".toByteArray())
        }
    }

    @Test
    fun parsesHttpsLinkTextAndSkipsInvalidNonBlankLines() {
        val text = """
            $validLink

            http://sky.example.test/badge?s=invalid
            https://example.test/not-a-badge
        """.trimIndent().toByteArray(Charsets.UTF_8)

        val parsed = QiguangBackupParser.parse(text)

        assertEquals(QiguangBackupParser.Format.HTTPS_LINKS, parsed.format)
        assertEquals(1, parsed.badges.size)
        assertEquals("SKU-123", parsed.badges.single().title)
        assertEquals(validLink, parsed.badges.single().link)
        assertEquals(BadgeChannel.SKY, parsed.badges.single().channel)
        assertEquals(2, parsed.skippedCount)
    }

    @Test
    fun parsesBadgeManagerJsonBackupAndPreservesBadgeData() {
        val source = listOf(
            Badge(
                id = "backup-id",
                title = "Saved badge",
                remark = "Saved note",
                link = validLink,
                channel = BadgeChannel.SKY,
                orderIndex = 7,
                tags = listOf("Event"),
            )
        )
        val output = ByteArrayOutputStream()
        BadgeBackupExporter.write(source) { output }

        val parsed = QiguangBackupParser.parse(output.toByteArray())

        assertEquals(QiguangBackupParser.Format.BADGE_MANAGER_JSON, parsed.format)
        assertEquals(source, parsed.badges)
        assertEquals(0, parsed.skippedCount)
    }

    @Test
    fun rejectsTextWithoutUsableHttpsBadgeLinks() {
        assertThrows(IllegalArgumentException::class.java) {
            QiguangBackupParser.parse("http://example.test/badge".toByteArray())
        }
    }

    @Test
    fun parsesXorWrappedDeflatedStarBackup() {
        val json = """
            {"backups":[{"name":"The Little Prince","url":"$validLink","note":"note","category":"Event"}]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val zipOutput = ByteArrayOutputStream()
        ZipOutputStream(zipOutput).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(json)
            zip.closeEntry()
        }
        val zipped = zipOutput.toByteArray()

        val parsed = QiguangBackupParser.parse(xorStar(zipped))

        assertEquals(QiguangBackupParser.Format.STAR, parsed.format)
        assertEquals("The Little Prince", parsed.badges.single().title)
        assertEquals("note", parsed.badges.single().remark)
        assertEquals(BadgeChannel.SKY, parsed.badges.single().channel)
        assertEquals(listOf("Event"), parsed.badges.single().tags)
    }

    @Test
    fun decryptsSkyBadgeShareOnceAndRequiresItsPassword() {
        val plaintext = """
            {"app":"sky-badge","version":1,"badges":[
              {"name":"Badge","profile":{"skyMeta":{"url":"$validLink"}}}
            ]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val encrypted = encryptedSkyBadge(plaintext, password = "secret", kind = "share")
        val envelopeText = encrypted.toString(Charsets.UTF_8)

        // Qiguang's current exporter uses standard Base64, including '+' and '/' characters.
        assertTrue(envelopeText.contains('+'))
        assertTrue(envelopeText.contains('/'))

        assertThrows(QiguangBackupParser.PasswordRequired::class.java) {
            QiguangBackupParser.parse(encrypted)
        }
        assertThrows(QiguangBackupParser.WrongPassword::class.java) {
            QiguangBackupParser.parse(encrypted, "incorrect")
        }
        val parsed = QiguangBackupParser.parse(encrypted, "secret")
        assertEquals("Badge", parsed.badges.single().title)
        assertTrue(parsed.encryptedShare)
    }

    @Test
    fun decryptsSkyBadgeShareWithUrlSafeBase64Variant() {
        val plaintext = """
            {"app":"sky-badge","version":1,"badges":[
              {"name":"Badge","profile":{"skyMeta":{"url":"$validLink"}}}
            ]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val encrypted = encryptedSkyBadge(
            plaintext,
            password = "secret",
            kind = "share",
            urlSafeBase64 = true,
        )

        val parsed = QiguangBackupParser.parse(encrypted, "secret")

        assertEquals("Badge", parsed.badges.single().title)
        assertTrue(parsed.encryptedShare)
    }

    @Test
    fun decryptsWinZipAesStarBackup() {
        val json = """
            {"backups":[{"name":"AES Badge","url":"$validLink"}]}
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val star = xorStar(winZipAesArchive(json, password = "star-secret"))

        assertThrows(QiguangBackupParser.PasswordRequired::class.java) {
            QiguangBackupParser.parse(star)
        }
        assertThrows(QiguangBackupParser.WrongPassword::class.java) {
            QiguangBackupParser.parse(star, "wrong")
        }
        val parsed = QiguangBackupParser.parse(star, "star-secret")
        assertEquals("AES Badge", parsed.badges.single().title)
    }

    @Test
    fun rejectsOversizedAndUnrelatedFiles() {
        assertThrows(IllegalArgumentException::class.java) {
            QiguangBackupParser.parse(ByteArray(QiguangBackupParser.MAX_FILE_BYTES + 1))
        }
        assertThrows(Exception::class.java) {
            QiguangBackupParser.parse("not a backup".toByteArray())
        }
    }

    private fun encryptedSkyBadge(
        plaintext: ByteArray,
        password: String,
        kind: String,
        urlSafeBase64: Boolean = false,
    ): ByteArray {
        val salt = ByteArray(16) { 0xff.toByte() }
        val nonce = ByteArray(16) { 0xfb.toByte() }
        val iterations = 1_000
        val derived = pbkdf2(password.toByteArray(Charsets.UTF_8), salt, iterations, 32, "HmacSHA256")
        val encKey = hmac("HmacSHA256", derived, "sky-badge/v2/enc".toByteArray())
        val macKey = hmac("HmacSHA256", derived, "sky-badge/v2/mac".toByteArray())
        val cipherText = Cipher.getInstance("AES/CTR/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(encKey, "AES"), IvParameterSpec(nonce))
            doFinal(plaintext)
        }
        val saltText = encodeBase64(salt, urlSafeBase64)
        val nonceText = encodeBase64(nonce, urlSafeBase64)
        val metadata = "2|$kind|AES-256-CTR+HMAC-SHA256|PBKDF2-SHA256|$iterations|$saltText|$nonceText|"
        val mac = hmac("HmacSHA256", macKey, metadata.toByteArray() + cipherText)
        return """
            {
              "app":"sky-badge","v":2,"kind":"$kind",
              "enc":{"alg":"AES-256-CTR+HMAC-SHA256","kdf":"PBKDF2-SHA256","iter":$iterations,"salt":"$saltText","nonce":"$nonceText"},
              "ct":"${encodeBase64(cipherText, urlSafeBase64)}","mac":"${encodeBase64(mac, urlSafeBase64)}"
            }
        """.trimIndent().toByteArray(Charsets.UTF_8)
    }

    private fun winZipAesArchive(plaintext: ByteArray, password: String): ByteArray {
        val name = "backup.json".toByteArray(Charsets.UTF_8)
        val salt = ByteArray(16) { (it + 9).toByte() }
        val keys = pbkdf2(password.toByteArray(Charsets.UTF_8), salt, 1_000, 66, "HmacSHA1")
        val verifier = keys.copyOfRange(64, 66)
        val cipherText = aesCtrLittleEndian(keys.copyOfRange(0, 32), plaintext)
        val auth = hmac("HmacSHA1", keys.copyOfRange(32, 64), cipherText).copyOf(10)
        val encryptedData = salt + verifier + cipherText + auth
        val aesExtra = ByteArrayOutputStream().apply {
            le16(0x9901)
            le16(7)
            le16(2)
            write('A'.code)
            write('E'.code)
            write(3)
            le16(0)
        }.toByteArray()

        val local = ByteArrayOutputStream().apply {
            le32(0x04034b50L)
            le16(20)
            le16(1)
            le16(99)
            le16(0)
            le16(0)
            le32(0)
            le32(encryptedData.size.toLong())
            le32(plaintext.size.toLong())
            le16(name.size)
            le16(aesExtra.size)
            write(name)
            write(aesExtra)
            write(encryptedData)
        }.toByteArray()
        val central = ByteArrayOutputStream().apply {
            le32(0x02014b50L)
            le16(20)
            le16(20)
            le16(1)
            le16(99)
            le16(0)
            le16(0)
            le32(0)
            le32(encryptedData.size.toLong())
            le32(plaintext.size.toLong())
            le16(name.size)
            le16(aesExtra.size)
            le16(0)
            le16(0)
            le16(0)
            le32(0)
            le32(0)
            write(name)
            write(aesExtra)
        }.toByteArray()
        val eocd = ByteArrayOutputStream().apply {
            le32(0x06054b50L)
            le16(0)
            le16(0)
            le16(1)
            le16(1)
            le32(central.size.toLong())
            le32(local.size.toLong())
            le16(0)
        }.toByteArray()
        return local + central + eocd
    }

    private fun aesCtrLittleEndian(key: ByteArray, input: ByteArray): ByteArray {
        val aes = Cipher.getInstance("AES/ECB/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        }
        val counter = ByteArray(16).apply { this[0] = 1 }
        val output = ByteArray(input.size)
        var offset = 0
        while (offset < input.size) {
            val stream = aes.doFinal(counter)
            val count = minOf(16, input.size - offset)
            repeat(count) { index -> output[offset + index] = (input[offset + index].toInt() xor stream[index].toInt()).toByte() }
            offset += count
            for (index in counter.indices) {
                counter[index] = (counter[index] + 1).toByte()
                if (counter[index].toInt() and 0xff != 0) break
            }
        }
        return output
    }

    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, size: Int, algorithm: String): ByteArray {
        val mac = Mac.getInstance(algorithm).apply { init(SecretKeySpec(password, algorithm)) }
        val result = ByteArray(size)
        val blocks = (size + mac.macLength - 1) / mac.macLength
        for (block in 1..blocks) {
            val input = salt + ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(block).array()
            var u = mac.doFinal(input)
            val aggregate = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                aggregate.indices.forEach { index -> aggregate[index] = (aggregate[index].toInt() xor u[index].toInt()).toByte() }
            }
            val offset = (block - 1) * mac.macLength
            System.arraycopy(aggregate, 0, result, offset, minOf(aggregate.size, size - offset))
        }
        return result
    }

    private fun hmac(algorithm: String, key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance(algorithm).run {
            init(SecretKeySpec(key, algorithm))
            doFinal(data)
        }

    private fun encodeBase64(bytes: ByteArray, urlSafe: Boolean): String =
        if (urlSafe) Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        else Base64.getEncoder().encodeToString(bytes)

    private fun xorStar(bytes: ByteArray): ByteArray {
        val key = "StarSkyXorKey".toByteArray(Charsets.UTF_8)
        return ByteArray(bytes.size) { index -> (bytes[index].toInt() xor key[index % key.size].toInt()).toByte() }
    }

    private fun ByteArrayOutputStream.le16(value: Int) {
        write(value and 0xff)
        write(value ushr 8 and 0xff)
    }

    private fun ByteArrayOutputStream.le32(value: Long) {
        repeat(4) { shift -> write((value ushr (shift * 8)).toInt() and 0xff) }
    }
}
