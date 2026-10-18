package io.github.tatooinoyo.star.badge.ui.home

import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BadgeOrderTest {
    @Test
    fun `same badge can be moved again after its index changes`() {
        val initial = badges("A", "B", "C", "D")

        val afterFirstMove = requireNotNull(
            reorderBadgesBefore(initial, badgeId = "A", insertBeforeId = null)
        )
        val afterSecondMove = requireNotNull(
            reorderBadgesBefore(afterFirstMove, badgeId = "A", insertBeforeId = "C")
        )

        assertEquals(listOf("B", "A", "C", "D"), afterSecondMove.map { it.id })
    }

    @Test
    fun `consecutive moves always resolve the current badge id`() {
        val initial = badges("A", "B", "C", "D")

        val afterFirstMove = requireNotNull(
            reorderBadgesBefore(initial, badgeId = "A", insertBeforeId = null)
        )
        val afterSecondMove = requireNotNull(
            reorderBadgesBefore(afterFirstMove, badgeId = "B", insertBeforeId = null)
        )

        assertEquals(listOf("C", "D", "A", "B"), afterSecondMove.map { it.id })
    }

    @Test
    fun `invalid or unchanged targets do not mutate the list`() {
        val initial = badges("A", "B", "C")

        assertNull(reorderBadgesBefore(initial, badgeId = "missing", insertBeforeId = null))
        assertNull(reorderBadgesBefore(initial, badgeId = "A", insertBeforeId = "missing"))
        assertNull(reorderBadgesBefore(initial, badgeId = "B", insertBeforeId = "B"))
        assertNull(reorderBadgesBefore(initial, badgeId = "B", insertBeforeId = "C"))
    }

    private fun badges(vararg ids: String): List<Badge> = ids.mapIndexed { index, id ->
        Badge(
            id = id,
            title = id,
            remark = "",
            channel = BadgeChannel.NETEASE,
            orderIndex = index,
        )
    }
}
