package io.github.tatooinoyo.star.badge.utils.export

import io.github.tatooinoyo.star.badge.data.Badge

object BadgeShareText {

    fun toEntries(badges: List<Badge>): List<Pair<String, String>> {
        return badges.map { badge ->
            badge.title.trim().ifBlank { "(untitled)" } to badge.link.trim()
        }.filter { (_, link) -> link.isNotEmpty() }
    }

    /** Human-readable: title + full link per badge. */
    fun formatPlain(badges: List<Badge>): String {
        return toEntries(badges).joinToString(separator = "\n\n") { (title, link) ->
            "$title\n$link"
        }
    }
}
