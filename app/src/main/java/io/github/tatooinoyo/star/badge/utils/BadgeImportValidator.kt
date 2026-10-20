package io.github.tatooinoyo.star.badge.utils

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel

/** Validate external JSON before constructing non-null Kotlin models or changing the database. */
object BadgeImportValidator {
    const val MAX_JSON_BYTES = 4 * 1024 * 1024
    const val MAX_BADGES = 10_000
    private const val MAX_TAGS = 32

    fun parse(json: String): List<Badge> {
        require(json.length <= MAX_JSON_BYTES && json.toByteArray(Charsets.UTF_8).size <= MAX_JSON_BYTES) {
            "Badge data is too large"
        }
        requireShallowJson(json)
        return parseArray(JsonParser.parseString(json))
    }

    fun requireShallowJson(json: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        for (char in json) {
            if (quoted) {
                if (escaped) escaped = false
                else if (char == '\\') escaped = true
                else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '[', '{' -> { depth++; require(depth <= 16) { "JSON nesting is too deep" } }
                ']', '}' -> depth--
            }
        }
    }

    fun parseArray(element: JsonElement): List<Badge> {
        require(element.isJsonArray) { "Expected a badge array" }
        val array = element.asJsonArray
        require(array.size() in 1..MAX_BADGES) { "Invalid badge count" }
        val ids = HashSet<String>()
        return array.map { entry ->
            require(entry.isJsonObject) { "Invalid badge" }
            val obj = entry.asJsonObject
            val id = obj.text("id", 128).also {
                require(it.isNotBlank() && ids.add(it)) { "Invalid or duplicate badge id" }
            }
            val channel = if (obj.has("channel")) {
                BadgeChannel.valueOf(obj.text("channel", 32))
            } else BadgeChannel.NETEASE
            val channelPackageName = obj.optionalText("channelPackageName", 256)
            val channelActivityName = obj.optionalText("channelActivityName", 512)
            val channelLabel = obj.optionalText("channelLabel", 256)
            val hasConcreteTarget = channelPackageName.isNotEmpty() ||
                channelActivityName.isNotEmpty() || channelLabel.isNotEmpty()
            if (channel == BadgeChannel.CUSTOM || hasConcreteTarget) {
                require(PACKAGE_NAME.matches(channelPackageName)) { "Invalid channel package" }
                require(channelActivityName.isNotBlank()) { "Invalid channel activity" }
                require(channelLabel.isNotBlank()) { "Invalid channel label" }
            }
            val tags = if (obj.has("tags")) {
                require(obj["tags"].isJsonArray) { "Invalid tags" }
                val values: JsonArray = obj.getAsJsonArray("tags")
                require(values.size() <= MAX_TAGS) { "Too many tags" }
                values.map { value -> string(value, 128).trim() }.filter { it.isNotEmpty() }.distinct()
            } else emptyList()
            val order = obj["orderIndex"]
            require(order != null && order.isJsonPrimitive && order.asJsonPrimitive.isNumber) { "Invalid order" }
            val orderIndex = order.asString.toIntOrNull() ?: error("Invalid order")
            Badge(
                id = id,
                title = obj.text("title", 512),
                remark = obj.text("remark", 4096),
                link = if (obj.has("link")) obj.text("link", 8192) else "",
                channel = channel,
                orderIndex = orderIndex,
                tags = tags,
                channelPackageName = channelPackageName,
                channelActivityName = channelActivityName,
                channelLabel = channelLabel,
            )
        }
    }

    private fun JsonObject.text(key: String, limit: Int): String =
        string(get(key) ?: error("Missing $key"), limit)

    private fun JsonObject.optionalText(key: String, limit: Int): String =
        if (has(key)) text(key, limit) else ""

    private fun string(value: JsonElement, limit: Int): String {
        require(value.isJsonPrimitive && value.asJsonPrimitive.isString) { "Expected a string" }
        return value.asString.also { require(it.length <= limit) { "Field is too long" } }
    }

    private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")
}
