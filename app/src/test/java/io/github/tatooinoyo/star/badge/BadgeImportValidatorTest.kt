package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator
import org.junit.Assert.*
import org.junit.Test

class BadgeImportValidatorTest {
    private val badge = """{"id":"a","title":"標題","remark":"","link":"https://example.test","channel":"NETEASE","orderIndex":0,"tags":["x","x"]}"""

    @Test fun validInputIsParsedAndTagsAreNormalized() {
        val parsed = BadgeImportValidator.parse("[$badge]").single()
        assertEquals("標題", parsed.title)
        assertEquals(listOf("x"), parsed.tags)
    }

    @Test fun legacyOptionalFieldsHaveExplicitDefaults() {
        val parsed = BadgeImportValidator.parse("""[{"id":"a","title":"t","remark":"","orderIndex":1}]""").single()
        assertEquals(BadgeChannel.NETEASE, parsed.channel)
        assertEquals(emptyList<String>(), parsed.tags)
        assertEquals("", parsed.link)
    }

    @Test fun emptyNullAndMalformedPayloadsCannotClearDatabase() {
        listOf("", "null", "[]", "{}", "[null]", "[$badge,", "[$badge,$badge]").forEach(::reject)
    }

    @Test fun invalidFieldsAreRejectedBeforeDatabaseWrites() {
        listOf(
            badge.replace("NETEASE", "FUTURE"),
            badge.replace("\"title\":\"標題\"", "\"title\":null"),
            badge.replace("\"id\":\"a\"", "\"id\":\"\""),
            badge.replace("\"orderIndex\":0", "\"orderIndex\":2147483648"),
            badge.replace("\"orderIndex\":0", "\"orderIndex\":0.5"),
            badge.replace("[\"x\",\"x\"]", "[null]"),
            badge.replace("[\"x\",\"x\"]", "null"),
        ).forEach { reject("[$it]") }
    }

    @Test fun excessiveLengthsCountsAndNestingAreRejected() {
        reject("[$badge]".replace("標題", "a".repeat(513)))
        reject("[" + (0..BadgeImportValidator.MAX_BADGES).joinToString(",") { badge.replace("\"a\"", "\"$it\"") } + "]")
        reject(" ".repeat(BadgeImportValidator.MAX_JSON_BYTES + 1))
        reject("[".repeat(1000) + "]".repeat(1000))
    }

    private fun reject(json: String) {
        assertThrows(RuntimeException::class.java) { BadgeImportValidator.parse(json) }
    }
}
