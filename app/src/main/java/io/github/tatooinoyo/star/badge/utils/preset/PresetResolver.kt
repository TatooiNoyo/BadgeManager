package io.github.tatooinoyo.star.badge.utils.preset

import android.content.Context
import io.github.tatooinoyo.star.badge.data.PresetBadges

/**
 * 候选徽章解析：内置 [PresetBadges] 优先，仅当内置无该 SK 时使用远程缓存。
 */
object PresetResolver {
    fun getTitle(context: Context, sk: String): String {
        val builtin = PresetBadges.getTitle(context, sk)
        if (builtin.isNotBlank()) return builtin
        return PresetRemoteStore.getTitle(sk)
    }

    fun getRemark(context: Context, sk: String): String {
        val builtinTitle = PresetBadges.getTitle(context, sk)
        if (builtinTitle.isNotBlank()) {
            return PresetBadges.getRemark(context, sk)
        }
        return PresetRemoteStore.getRemark(sk)
    }

    fun isRecorded(context: Context, sk: String): Boolean {
        return getTitle(context, sk).isNotBlank()
    }

    fun resolve(context: Context, sk: String): Pair<String, String>? {
        val title = getTitle(context, sk)
        if (title.isBlank()) return null
        return title to getRemark(context, sk)
    }
}
