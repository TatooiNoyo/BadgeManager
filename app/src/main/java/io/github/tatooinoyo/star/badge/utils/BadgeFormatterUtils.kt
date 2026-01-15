package io.github.tatooinoyo.star.badge.utils

import android.content.Context
import io.github.tatooinoyo.star.badge.data.Badge

object BadgeFormatterUtils {
    fun formatUnrecordedBadges(context: Context, badges: List<Badge>): String {

        return buildString {
            badges.forEachIndexed { index, badge ->
                append("${index + 1}. \n${badge.title}\n")
                if (badge.remark.isNotBlank()) {
                    append("${badge.remark}\n")
                }
                val skCode = SkExtractor.getSkFromLink(badge.link)
                if (skCode.isNotBlank()) {
                    append("$skCode\n")
                }
                if (index < badges.size - 1) append("\n")
            }
        }
    }
}