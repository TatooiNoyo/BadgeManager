package io.github.tatooinoyo.star.badge.ui.home

import io.github.tatooinoyo.star.badge.data.Badge

/**
 * 将 [badgeId] 移到 [insertBeforeId] 之前；目标为 null 时移到末尾。
 * 返回 null 表示来源、目标无效或顺序没有变化。
 */
internal fun reorderBadgesBefore(
    badges: List<Badge>,
    badgeId: String,
    insertBeforeId: String?,
): List<Badge>? {
    if (badgeId == insertBeforeId) return null

    val from = badges.indexOfFirst { it.id == badgeId }
    if (from < 0) return null

    val reordered = badges.toMutableList()
    val badge = reordered.removeAt(from)
    val target = if (insertBeforeId == null) {
        reordered.size
    } else {
        reordered.indexOfFirst { it.id == insertBeforeId }
            .takeIf { it >= 0 }
            ?: return null
    }
    reordered.add(target, badge)

    return reordered.takeUnless { it == badges }
}
