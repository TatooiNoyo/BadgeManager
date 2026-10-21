package io.github.tatooinoyo.star.badge.utils

import android.annotation.SuppressLint
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.Locale
import java.util.zip.CRC32
import java.util.zip.DataFormatException
import java.util.zip.Inflater
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** Reads the public JSON and Star archive formats exported by Qiguang. */
object QiguangBackupParser {
    const val MAX_FILE_BYTES = 16 * 1024 * 1024
    private const val MAX_JSON_BYTES = 8 * 1024 * 1024
    private const val MAX_BADGES = 10_000
    private const val MAX_DEFLATED_BYTES = 8 * 1024 * 1024
    private const val MAX_TEXT_FIELD = 8_192
    private const val STAR_XOR_KEY = "StarSkyXorKey"
    private val utf8 = StandardCharsets.UTF_8

    enum class Format(val displayName: String) {
        SKYBADGE(".skybadge / JSON"),
        STAR(".star"),
        BADGE_MANAGER_JSON("BadgeManager JSON backup"),
        HTTPS_LINKS("HTTPS links (.txt)"),
    }

    data class ParsedImport(
        val badges: List<Badge>,
        val skippedCount: Int,
        val encryptedShare: Boolean = false,
        val format: Format,
    )

    class PasswordRequired(val format: Format) : IOException("Password required")
    class WrongPassword : IOException("The password is incorrect or the file is damaged")

    /** Detects Qiguang's XOR-wrapped ZIP signature without trusting the extension. */
    fun looksLikeStar(data: ByteArray): Boolean = data.size >= 4 &&
        readXorByte(data, 0) == 0x50 && readXorByte(data, 1) == 0x4b &&
        readXorByte(data, 2) == 0x03 && readXorByte(data, 3) == 0x04

    fun parse(data: ByteArray, password: String? = null): ParsedImport {
        require(data.isNotEmpty()) { "File is empty" }
        require(data.size <= MAX_FILE_BYTES) { "File exceeds the 16 MB import limit" }
        return if (looksLikeStar(data)) {
            parseStar(data, password)
        } else {
            val text = decodeText(data).removePrefix("\uFEFF").trim()
            if (text.startsWith('{') || text.startsWith('[') || text.startsWith('"')) {
                val root = decodeJson(data)
                if (isBadgeManagerBackup(root)) {
                    val badges = BadgeImportValidator.parseArray(root)
                    ParsedImport(badges, skippedCount = 0, format = Format.BADGE_MANAGER_JSON)
                } else {
                    parseSkyBadge(root, password, Format.SKYBADGE)
                }
            } else {
                parseHttpsLinks(text)
            }
        }
    }

    private fun isBadgeManagerBackup(root: JsonElement): Boolean =
        root.isJsonArray && root.asJsonArray.firstOrNull()?.let { entry ->
            entry.isJsonObject && entry.asJsonObject.has("id") &&
                entry.asJsonObject.has("title") && entry.asJsonObject.has("orderIndex")
        } == true

    private fun parseHttpsLinks(text: String): ParsedImport {
        require(text.toByteArray(utf8).size <= MAX_JSON_BYTES) { "The text file is too large" }
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        require(lines.size <= MAX_BADGES) { "Too many badge links" }

        val badges = ArrayList<Badge>(lines.size)
        var skipped = 0
        lines.forEach { link ->
            val sku = if (link.length <= MAX_TEXT_FIELD && isHttps(link)) linkSku(link) else null
            if (sku.isNullOrBlank()) {
                skipped++
            } else {
                badges += Badge(
                    title = sku,
                    remark = "",
                    link = link,
                    channel = BadgeChannel.SKY,
                    orderIndex = 0,
                    tags = emptyList(),
                )
            }
        }
        require(badges.isNotEmpty()) { "No usable HTTPS badge links were found in this file" }
        return ParsedImport(badges, skipped, format = Format.HTTPS_LINKS)
    }

    private fun isHttps(value: String): Boolean = try {
        URI(value).scheme.equals("https", ignoreCase = true)
    } catch (_: Exception) {
        false
    }

    private fun parseSkyBadge(
        root: JsonElement,
        password: String?,
        format: Format,
    ): ParsedImport {
        if (root.isJsonObject) {
            val envelope = root.asJsonObject
            val app = envelope.stringOrNull("app")
            require(app == null || app == "sky-badge") { "This JSON file is not a Qiguang backup" }
            if (envelope.has("enc") || envelope.has("ct") || envelope.has("mac")) {
                val decrypted = decryptSkyBadgeEnvelope(envelope, password)
                val parsed = parseSkyBadge(decodeJson(decrypted.plaintext), password, format)
                return parsed.copy(encryptedShare = decrypted.kind == "share", format = format)
            }
        }

        val objectRoot = root.takeIf { it.isJsonObject }?.asJsonObject
        if (objectRoot?.has("badges") == true && objectRoot.has("version")) {
            val version = objectRoot.get("version")
            require(version.isJsonPrimitive && version.asJsonPrimitive.isNumber && version.asInt == 1) {
                "This Qiguang backup version is not supported"
            }
        }

        val entries = when {
            root.isJsonArray -> root.asJsonArray.toList()
            objectRoot?.has("badges") == true -> {
                val badges = objectRoot.get("badges")
                require(badges.isJsonArray) { "Qiguang badge list is invalid" }
                badges.asJsonArray.toList()
            }
            root.isJsonObject -> listOf(root)
            else -> error("No Qiguang badge data found")
        }
        require(entries.isNotEmpty() && entries.size <= MAX_BADGES) { "Invalid badge count" }

        val categoryLabels = categoryMap(objectRoot)
        val assignments = assignmentMap(objectRoot)
        val badges = ArrayList<Badge>(entries.size)
        var skipped = 0
        entries.forEach { rawEntry ->
            val entry = try {
                decodeEntry(rawEntry)
            } catch (_: Exception) {
                skipped++
                return@forEach
            }
            val profile = entry.objectOrNull("profile")
            val meta = profile?.objectOrNull("skyMeta")
            val link = meta?.stringOrNull("url")?.trim().orEmpty()
            val sku = meta?.stringOrNull("sku")?.trim().orEmpty()
            if (link.isBlank() || link.length > MAX_TEXT_FIELD || linkSku(link).isNullOrBlank()) {
                skipped++
                return@forEach
            }

            val categoryId = entry.stringOrNull("categoryId")
                ?: assignments[entry.stringOrNull("id")]
            val category = categoryId?.let(categoryLabels::get)
            val tags = buildList {
                category?.trim()?.takeIf { it.isNotEmpty() && it.length <= 128 }?.let(::add)
                entry.get("tags")?.takeIf(JsonElement::isJsonArray)?.asJsonArray?.forEach { tag ->
                    if (tag.isJsonPrimitive && tag.asJsonPrimitive.isString) {
                        tag.asString.trim().takeIf { it.isNotEmpty() && it.length <= 128 }?.let(::add)
                    }
                }
            }.distinct().take(32)

            val title = (entry.stringOrNull("name") ?: entry.stringOrNull("title"))
                ?.trim().orEmpty().ifBlank { sku.ifBlank { linkSku(link).orEmpty() } }
            val remark = (entry.stringOrNull("note") ?: entry.stringOrNull("remark"))
                ?.trim().orEmpty()
            if (title.length > 512 || remark.length > 4_096) {
                skipped++
                return@forEach
            }
            badges += Badge(
                title = title,
                remark = remark,
                link = link,
                channel = BadgeChannel.SKY,
                orderIndex = 0,
                tags = tags,
            )
        }
        require(badges.isNotEmpty()) { "No usable badge links were found in this file" }
        return ParsedImport(badges, skipped, format = format)
    }

    private fun decodeEntry(element: JsonElement): JsonObject {
        val parsed = if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
            JsonParser.parseString(element.asString)
        } else element
        require(parsed.isJsonObject) { "Invalid badge entry" }
        return parsed.asJsonObject
    }

    private fun parseStar(data: ByteArray, password: String?): ParsedImport {
        val archive = ByteArray(data.size) { index -> readXorByte(data, index).toByte() }
        val jsonBytes = readStarEntry(archive, password)
        val root = decodeJson(jsonBytes)
        require(root.isJsonObject) { "The Star backup does not contain badge data" }
        val backup = root.asJsonObject
        val records = backup.get("backups")
        require(records != null && records.isJsonArray) { "The Star backup has no badge list" }
        require(records.asJsonArray.size() in 1..MAX_BADGES) { "Invalid badge count" }

        val categoryLabels = backup.get("categories")?.takeIf(JsonElement::isJsonArray)
            ?.asJsonArray?.mapNotNull { value ->
                if (value.isJsonPrimitive && value.asJsonPrimitive.isString) value.asString else null
            }.orEmpty()
        val badges = ArrayList<Badge>(records.asJsonArray.size())
        var skipped = 0
        records.asJsonArray.forEach { raw ->
            if (!raw.isJsonObject) {
                skipped++
                return@forEach
            }
            val record = raw.asJsonObject
            val link = record.stringOrNull("url")?.trim().orEmpty()
            val sku = linkSku(link)
            if (link.isBlank() || sku.isNullOrBlank()) {
                skipped++
                return@forEach
            }
            val title = record.stringOrNull("name")?.trim().orEmpty().ifBlank { sku }
            val note = record.stringOrNull("note")?.trim().orEmpty()
            if (title.length > 512 || note.length > 4_096) {
                skipped++
                return@forEach
            }
            val category = record.stringOrNull("category")?.trim().orEmpty()
                .ifBlank {
                    record.stringOrNull("category_id")?.toIntOrNull()
                        ?.let(categoryLabels::getOrNull).orEmpty()
                }
            badges += Badge(
                title = title,
                remark = note,
                link = link,
                channel = BadgeChannel.SKY,
                orderIndex = 0,
                tags = listOfNotNull(category.takeIf { it.isNotBlank() && it.length <= 128 }),
            )
        }
        require(badges.isNotEmpty()) { "No usable badge links were found in this Star backup" }
        return ParsedImport(badges, skipped, format = Format.STAR)
    }

    private fun readStarEntry(archive: ByteArray, password: String?): ByteArray {
        require(archive.size >= 30 && archive.readLe32(0) == 0x04034b50L) {
            "The Star backup is not a supported ZIP archive"
        }
        val flags = archive.readLe16(6)
        val zipMethod = archive.readLe16(8)
        val nameLength = archive.readLe16(26)
        val extraLength = archive.readLe16(28)
        val headerEnd = 30L + nameLength + extraLength
        require(headerEnd <= archive.size) { "The Star backup is incomplete" }
        val nameBytes = archive.copyOfRange(30, 30 + nameLength)
        val name = String(nameBytes, utf8)
        val dataOffset = headerEnd.toInt()
        val localCompressedSize = archive.readLe32(18)
        val localUncompressedSize = archive.readLe32(22)
        val (compressedSize, uncompressedSize, expectedCrc) = if (
            localCompressedSize == 0L || localUncompressedSize == 0L || flags and 0x08 != 0
        ) {
            findCentralEntry(archive, name)
        } else {
            Triple(localCompressedSize, localUncompressedSize, archive.readLe32(14))
        }
        require(compressedSize != 0xffff_ffffL && uncompressedSize != 0xffff_ffffL) {
            "ZIP64 Star backups are not supported"
        }
        require(compressedSize <= MAX_FILE_BYTES && uncompressedSize <= MAX_DEFLATED_BYTES) {
            "The Star backup contents are too large"
        }
        val dataEnd = dataOffset.toLong() + compressedSize
        require(dataEnd <= archive.size) { "The Star backup is incomplete" }
        val aes = if (zipMethod == 99) parseAesExtra(archive, 30 + nameLength, extraLength) else null
        val compressed = when {
            aes != null -> {
                if (flags and 1 == 0) throw IOException("The encrypted Star archive is malformed")
                val pass = password?.takeIf(String::isNotEmpty) ?: throw PasswordRequired(Format.STAR)
                decryptWinZipAes(
                    archive.copyOfRange(dataOffset, dataEnd.toInt()),
                    aes,
                    pass,
                )
            }
            flags and 1 != 0 -> throw IOException("This Star archive uses an unsupported encryption method")
            else -> archive.copyOfRange(dataOffset, dataEnd.toInt())
        }
        val method = aes?.compressionMethod ?: zipMethod
        val content = when (method) {
            0 -> compressed
            8 -> inflateRaw(compressed, uncompressedSize.toInt())
            else -> throw IOException("This Star archive uses an unsupported compression method")
        }
        require(content.size.toLong() == uncompressedSize || uncompressedSize == 0L) {
            "The Star backup has an invalid uncompressed size"
        }
        if (aes == null && expectedCrc != 0L) {
            val crc = CRC32().apply { update(content) }.value
            require(crc == expectedCrc) { "The Star backup failed its CRC check" }
        }
        require(name.isNotBlank()) { "The Star backup entry has no name" }
        return content
    }

    private data class AesExtra(val strength: Int, val compressionMethod: Int)

    private fun parseAesExtra(archive: ByteArray, offset: Int, size: Int): AesExtra {
        var cursor = offset
        val end = offset + size
        while (cursor + 4 <= end) {
            val id = archive.readLe16(cursor)
            val length = archive.readLe16(cursor + 2)
            val dataStart = cursor + 4
            require(dataStart + length <= end) { "The Star AES metadata is malformed" }
            if (id == 0x9901) {
                require(length >= 7) { "The Star AES metadata is incomplete" }
                val version = archive.readLe16(dataStart)
                val vendor = String(archive, dataStart + 2, 2, StandardCharsets.US_ASCII)
                val strength = archive[dataStart + 4].toInt() and 0xff
                require(version in 1..2 && vendor == "AE" && strength in 1..3) {
                    "The Star archive uses an unsupported AES variant"
                }
                return AesExtra(strength, archive.readLe16(dataStart + 5))
            }
            cursor = dataStart + length
        }
        throw IOException("The Star AES metadata is missing")
    }

    private fun decryptWinZipAes(data: ByteArray, aes: AesExtra, password: String): ByteArray {
        val keyLength = when (aes.strength) {
            1 -> 16
            2 -> 24
            else -> 32
        }
        val saltLength = keyLength / 2
        require(data.size >= saltLength + 12) { "The encrypted Star backup is incomplete" }
        val salt = data.copyOfRange(0, saltLength)
        val authCode = data.copyOfRange(data.size - 10, data.size)
        val keys = pbkdf2(password.toByteArray(utf8), salt, 1_000, 2 * keyLength + 2, "HmacSHA1")
        val encryptionKey = keys.copyOfRange(0, keyLength)
        val authenticationKey = keys.copyOfRange(keyLength, 2 * keyLength)
        val firstPayload = data.copyOfRange(saltLength + 2, data.size - 10)
        val secondPayload = data.copyOfRange(saltLength, data.size - 10)
        val payload = when {
            constantTimeEquals(hmac("HmacSHA1", authenticationKey, firstPayload).copyOf(10), authCode) -> firstPayload
            constantTimeEquals(hmac("HmacSHA1", authenticationKey, secondPayload).copyOf(10), authCode) -> secondPayload
            else -> throw WrongPassword()
        }
        return aesCtrLittleEndian(encryptionKey, payload)
    }

    // WinZip AES requires CTR with a little-endian counter; ECB is only used to encrypt counter blocks.
    @SuppressLint("GetInstance")
    private fun aesCtrLittleEndian(key: ByteArray, input: ByteArray): ByteArray {
        val aes = Cipher.getInstance("AES/ECB/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        }
        val output = ByteArray(input.size)
        val counter = ByteArray(16).apply { this[0] = 1 }
        var offset = 0
        while (offset < input.size) {
            val keyStream = aes.doFinal(counter)
            val count = minOf(16, input.size - offset)
            for (index in 0 until count) output[offset + index] = (input[offset + index].toInt() xor keyStream[index].toInt()).toByte()
            offset += count
            for (index in counter.indices) {
                counter[index] = (counter[index] + 1).toByte()
                if (counter[index].toInt() and 0xff != 0) break
            }
        }
        return output
    }

    private fun findCentralEntry(archive: ByteArray, name: String): Triple<Long, Long, Long> {
        val earliest = maxOf(0, archive.size - 65_557)
        var eocd = -1
        for (offset in archive.size - 22 downTo earliest) {
            if (archive.readLe32(offset) == 0x06054b50L) {
                eocd = offset
                break
            }
        }
        require(eocd >= 0) { "The Star backup is incomplete" }
        val entries = archive.readLe16(eocd + 10)
        val centralOffset = archive.readLe32(eocd + 16)
        require(entries != 0xffff && centralOffset != 0xffff_ffffL) { "ZIP64 Star backups are not supported" }
        var cursor = centralOffset.toInt()
        repeat(entries) {
            require(cursor + 46 <= archive.size && archive.readLe32(cursor) == 0x02014b50L) {
                "The Star backup central directory is malformed"
            }
            val crc = archive.readLe32(cursor + 16)
            val compressed = archive.readLe32(cursor + 20)
            val uncompressed = archive.readLe32(cursor + 24)
            val entryNameLength = archive.readLe16(cursor + 28)
            val extraLength = archive.readLe16(cursor + 30)
            val commentLength = archive.readLe16(cursor + 32)
            val entryNameStart = cursor + 46
            val next = entryNameStart + entryNameLength + extraLength + commentLength
            require(next <= archive.size) { "The Star backup central directory is truncated" }
            val entryName = String(archive, entryNameStart, entryNameLength, utf8)
            if (entryName == name && !name.endsWith('/')) return Triple(compressed, uncompressed, crc)
            cursor = next
        }
        throw IOException("The Star backup entry was not found")
    }

    private fun inflateRaw(input: ByteArray, expectedSize: Int): ByteArray {
        val inflater = Inflater(true)
        inflater.setInput(input)
        val output = ByteArrayOutputStream(minOf(expectedSize.coerceAtLeast(0), 64 * 1024))
        val buffer = ByteArray(8 * 1024)
        try {
            while (!inflater.finished()) {
                val count = try {
                    inflater.inflate(buffer)
                } catch (error: DataFormatException) {
                    throw IOException("The Star backup DEFLATE stream is invalid", error)
                }
                if (count > 0) {
                    require(output.size() <= MAX_DEFLATED_BYTES - count) { "The Star backup contents are too large" }
                    output.write(buffer, 0, count)
                } else if (inflater.needsDictionary() || inflater.needsInput()) {
                    throw IOException("The Star backup DEFLATE stream is incomplete")
                } else {
                    throw IOException("The Star backup DEFLATE stream cannot continue")
                }
            }
            require(inflater.remaining == 0) { "The Star backup contains unexpected trailing data" }
        } finally {
            inflater.end()
        }
        return output.toByteArray()
    }

    private fun decodeJson(data: ByteArray): JsonElement {
        require(data.size <= MAX_JSON_BYTES) { "The backup JSON is too large" }
        var text = decodeText(data).removePrefix("\uFEFF").trim()
        repeat(3) {
            val element = try {
                JsonParser.parseString(text)
            } catch (error: Exception) {
                throw IOException("This is not valid Qiguang JSON", error)
            }
            if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                text = element.asString.removePrefix("\uFEFF").trim()
            } else {
                requireShallowJson(text)
                return element
            }
        }
        throw IOException("The JSON file contains too many nested string wrappers")
    }

    private fun decodeText(data: ByteArray): String = when {
        data.size >= 2 && data[0] == 0xff.toByte() && data[1] == 0xfe.toByte() ->
            String(data, 2, data.size - 2, StandardCharsets.UTF_16LE)
        data.size >= 2 && data[0] == 0xfe.toByte() && data[1] == 0xff.toByte() ->
            String(data, 2, data.size - 2, StandardCharsets.UTF_16BE)
        data.size >= 3 && data[0] == 0xef.toByte() && data[1] == 0xbb.toByte() && data[2] == 0xbf.toByte() ->
            decodeUtf8(data, 3)
        else -> decodeUtf8(data, 0)
    }

    private fun decodeUtf8(data: ByteArray, offset: Int): String = try {
        utf8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(data, offset, data.size - offset)).toString()
    } catch (error: Exception) {
        throw IOException("The backup text encoding is invalid", error)
    }

    private fun requireShallowJson(json: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        json.forEach { char ->
            if (quoted) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '[', '{' -> {
                    depth++
                    require(depth <= 16) { "The JSON nesting is too deep" }
                }
                ']', '}' -> depth--
            }
        }
    }

    private data class SkyBadgeDecryption(val kind: String, val plaintext: ByteArray)

    private fun decryptSkyBadgeEnvelope(
        envelope: JsonObject,
        password: String?,
    ): SkyBadgeDecryption {
        require(envelope.stringOrNull("app") == "sky-badge" && envelope.intOrNull("v") == 2) {
            "This encrypted Qiguang backup version is not supported"
        }
        val kind = envelope.stringOrNull("kind")
        require(kind == "backup" || kind == "share") { "The encrypted Qiguang file type is invalid" }
        val enc = envelope.objectOrNull("enc") ?: error("Encrypted backup metadata is missing")
        require(enc.stringOrNull("alg") == "AES-256-CTR+HMAC-SHA256" &&
            enc.stringOrNull("kdf") == "PBKDF2-SHA256") { "The encrypted Qiguang algorithm is not supported" }
        val iterations = enc.intOrNull("iter") ?: error("The encrypted Qiguang parameters are invalid")
        require(iterations in 1_000..1_000_000) { "The encrypted Qiguang parameters are invalid" }
        val salt = decodeBase64(enc.stringOrNull("salt") ?: error("The encrypted Qiguang salt is missing"))
        val nonce = decodeBase64(enc.stringOrNull("nonce") ?: error("The encrypted Qiguang nonce is missing"))
        val cipherText = decodeBase64(envelope.stringOrNull("ct") ?: error("The encrypted Qiguang payload is missing"))
        val suppliedMac = decodeBase64(envelope.stringOrNull("mac") ?: error("The encrypted Qiguang signature is missing"))
        require(salt.size == 16 && nonce.size == 16 && suppliedMac.size == 32 && cipherText.isNotEmpty()) {
            "The encrypted Qiguang file is malformed"
        }
        val pass = password?.takeIf(String::isNotEmpty) ?: throw PasswordRequired(Format.SKYBADGE)
        require(pass.length <= 1_024) { "The password is too long" }
        val derived = pbkdf2(pass.toByteArray(utf8), salt, iterations, 32, "HmacSHA256")
        val encryptionKey = hmac("HmacSHA256", derived, "sky-badge/v2/enc".toByteArray(utf8))
        val authenticationKey = hmac("HmacSHA256", derived, "sky-badge/v2/mac".toByteArray(utf8))
        val metadata = "2|$kind|AES-256-CTR+HMAC-SHA256|PBKDF2-SHA256|$iterations|${enc.stringOrNull("salt")}|${enc.stringOrNull("nonce")}|"
        val expectedMac = hmac("HmacSHA256", authenticationKey, metadata.toByteArray(utf8) + cipherText)
        if (!constantTimeEquals(expectedMac, suppliedMac)) throw WrongPassword()
        val plain = Cipher.getInstance("AES/CTR/NoPadding").run {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(encryptionKey, "AES"), IvParameterSpec(nonce))
            doFinal(cipherText)
        }
        return SkyBadgeDecryption(kind, plain)
    }

    private fun categoryMap(root: JsonObject?): Map<String, String> {
        val categories = root?.get("organization")?.takeIf(JsonElement::isJsonObject)
            ?.asJsonObject?.get("categories") ?: root?.get("categories")
        if (categories?.isJsonArray != true) return emptyMap()
        return buildMap {
            categories.asJsonArray.forEach { category ->
                if (category.isJsonObject) {
                    val item = category.asJsonObject
                    val id = item.stringOrNull("id")?.trim().orEmpty()
                    val label = item.stringOrNull("label")?.trim().orEmpty()
                    if (id.isNotEmpty() && label.isNotEmpty()) put(id, label)
                }
            }
        }
    }

    private fun assignmentMap(root: JsonObject?): Map<String, String> {
        val assignments = root?.objectOrNull("organization")?.get("assignments")
        if (assignments?.isJsonArray != true) return emptyMap()
        return buildMap {
            assignments.asJsonArray.forEach { assignment ->
                if (assignment.isJsonObject) {
                    val item = assignment.asJsonObject
                    val id = item.stringOrNull("id")
                    val categoryId = item.stringOrNull("categoryId")
                    if (!id.isNullOrBlank() && !categoryId.isNullOrBlank()) put(id, categoryId)
                }
            }
        }
    }

    /** Returns the SKU only when the Qiguang URL contains both SKU and token identity. */
    private fun linkSku(value: String): String? {
        return try {
            val uri = URI(value)
            if (uri.scheme?.lowercase(Locale.ROOT) !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
            val outer = queryParameters(uri.rawQuery.orEmpty())
            val encoded = outer["s"] ?: return null
            val payload = String(Base64.getUrlDecoder().decode(encoded), utf8)
            val inner = queryParameters(payload)
            inner["sk"]?.takeIf(String::isNotBlank)?.takeIf { !inner["s"].isNullOrBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryParameters(query: String): Map<String, String> = buildMap {
        query.split('&').forEach { part ->
            if (part.isEmpty()) return@forEach
            val separator = part.indexOf('=')
            val key = if (separator < 0) part else part.substring(0, separator)
            val value = if (separator < 0) "" else part.substring(separator + 1)
            put(URLDecoder.decode(key, utf8.name()), URLDecoder.decode(value, utf8.name()))
        }
    }

    private fun pbkdf2(password: ByteArray, salt: ByteArray, iterations: Int, size: Int, algorithm: String): ByteArray {
        require(iterations > 0 && size > 0)
        val mac = Mac.getInstance(algorithm)
        mac.init(SecretKeySpec(password, algorithm))
        val output = ByteArray(size)
        val blocks = (size + mac.macLength - 1) / mac.macLength
        for (block in 1..blocks) {
            val saltAndIndex = ByteArray(salt.size + 4)
            System.arraycopy(salt, 0, saltAndIndex, 0, salt.size)
            ByteBuffer.wrap(saltAndIndex, salt.size, 4).order(ByteOrder.BIG_ENDIAN).putInt(block)
            var u = mac.doFinal(saltAndIndex)
            val aggregate = u.copyOf()
            repeat(iterations - 1) {
                u = mac.doFinal(u)
                for (index in aggregate.indices) aggregate[index] = (aggregate[index].toInt() xor u[index].toInt()).toByte()
            }
            val offset = (block - 1) * mac.macLength
            System.arraycopy(aggregate, 0, output, offset, minOf(aggregate.size, size - offset))
        }
        return output
    }

    private fun hmac(algorithm: String, key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance(algorithm).run {
            init(SecretKeySpec(key, algorithm))
            doFinal(data)
        }

    private fun constantTimeEquals(first: ByteArray, second: ByteArray): Boolean {
        if (first.size != second.size) return false
        var difference = 0
        first.indices.forEach { difference = difference or (first[it].toInt() xor second[it].toInt()) }
        return difference == 0
    }

    /** Qiguang emits padded standard Base64, while accepting URL-safe Base64 is useful for variants. */
    private fun decodeBase64(value: String): ByteArray = try {
        Base64.getDecoder().decode(value.replace('-', '+').replace('_', '/'))
    } catch (error: IllegalArgumentException) {
        throw IOException("The encrypted Qiguang backup contains invalid data", error)
    }

    private fun readXorByte(data: ByteArray, index: Int): Int =
        (data[index].toInt() and 0xff) xor (STAR_XOR_KEY[index % STAR_XOR_KEY.length].code and 0xff)

    private fun ByteArray.readLe16(offset: Int): Int {
        require(offset >= 0 && offset + 2 <= size) { "The archive is truncated" }
        return (this[offset].toInt() and 0xff) or ((this[offset + 1].toInt() and 0xff) shl 8)
    }

    private fun ByteArray.readLe32(offset: Int): Long {
        require(offset >= 0 && offset + 4 <= size) { "The archive is truncated" }
        return (this[offset].toLong() and 0xff) or
            ((this[offset + 1].toLong() and 0xff) shl 8) or
            ((this[offset + 2].toLong() and 0xff) shl 16) or
            ((this[offset + 3].toLong() and 0xff) shl 24)
    }
}

private fun JsonObject.stringOrNull(key: String): String? =
    get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString

private fun JsonObject.intOrNull(key: String): Int? = try {
    get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
} catch (_: Exception) {
    null
}

private fun JsonObject.objectOrNull(key: String): JsonObject? =
    get(key)?.takeIf(JsonElement::isJsonObject)?.asJsonObject
