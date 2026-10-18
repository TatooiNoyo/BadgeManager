package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.input.pointer.PointerId
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiTouchReorderStateTest {
    @Test
    fun `active drag rejects another pointer and only owner can cancel`() {
        val state = MultiTouchReorderState(
            listState = LazyListState(),
            onMove = { _, _ -> true },
            onSaveOrder = {},
        )
        state.updateItemKeys(listOf("A", "B"))
        val firstPointer = PointerId(1)
        val secondPointer = PointerId(2)

        assertTrue(state.onDragStart(badge("A"), 10f, firstPointer, 50f))
        assertFalse(state.onDragStart(badge("B"), 20f, secondPointer, 50f))

        state.onDragCancel(secondPointer)
        assertTrue(state.isDragging)

        state.onDragCancel(firstPointer)
        assertFalse(state.isDragging)
    }

    private fun badge(id: String) = Badge(
        id = id,
        title = id,
        remark = "",
        channel = BadgeChannel.NETEASE,
        orderIndex = 0,
    )
}
