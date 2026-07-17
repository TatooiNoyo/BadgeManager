package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.layout.LayoutCoordinates
import io.github.tatooinoyo.star.badge.data.Badge

/**
 * 拖拽排序状态：拖动时从列表中暂隐被拖项（下方补位），悬浮层跟随手指；
 * 松手后按横杆落点一次性 onMove + onSaveOrder。
 *
 * 拖拽期间列表展示的是去掉被拖项后的序列，[dropInsertBeforeIndex] 即
 * MutableList.add(to, removeAt(from)) 中的 to。
 */
@Stable
class MultiTouchReorderState(
    private val listState: LazyListState,
    private val onMove: (Int, Int) -> Unit,
    private val onSaveOrder: () -> Unit,
) {
    var isDragging by mutableStateOf(false)
        private set

    var draggingKey by mutableStateOf<String?>(null)
        private set

    var draggingBadge by mutableStateOf<Badge?>(null)
        private set

    var draggingIndex by mutableIntStateOf(-1)
        private set

    var dragPointerId by mutableStateOf(PointerId(-1))
        private set

    var dragFingerY by mutableFloatStateOf(0f)
        private set

    var draggingItemHeight by mutableFloatStateOf(0f)
        private set

    /** 落点：插入到「已去掉被拖项」列表的该下标（0..itemCount） */
    var dropInsertBeforeIndex by mutableIntStateOf(-1)
        private set

    /** 横杆在 LazyColumn 视口坐标系中的 Y */
    var dropIndicatorY by mutableFloatStateOf(0f)
        private set

    /** 当前列表展示条数（拖拽中为原数量 - 1） */
    var itemCount by mutableIntStateOf(0)

    var listCoordinates: LayoutCoordinates? = null

    fun onDragStart(
        badge: Badge,
        index: Int,
        fingerY: Float,
        pointerId: PointerId,
        itemHeight: Float,
    ) {
        isDragging = true
        draggingKey = badge.id
        draggingBadge = badge
        draggingIndex = index
        dragFingerY = fingerY
        dragPointerId = pointerId
        draggingItemHeight = itemHeight
        resolveDropTarget()
    }

    fun updateDragFingerY(fingerY: Float) {
        if (!isDragging) return
        dragFingerY = fingerY
        resolveDropTarget()
    }

    fun onListScrolled() {
        resolveDropTarget()
    }

    private fun resolveDropTarget() {
        if (!isDragging || draggingIndex < 0) return
        val resolved = resolveInsertBeforeIndex(listState, dragFingerY, itemCount) ?: return
        dropInsertBeforeIndex = resolved.index
        dropIndicatorY = resolved.indicatorY
    }

    fun onDragEnd() {
        if (!isDragging) return
        val from = draggingIndex
        val to = dropInsertBeforeIndex
        isDragging = false
        draggingKey = null
        draggingBadge = null
        draggingIndex = -1
        dragPointerId = PointerId(-1)
        dropInsertBeforeIndex = -1
        draggingItemHeight = 0f

        // 展示列表已不含被拖项，to 直接对应 add(to, removeAt(from))；to == from 表示落回原位
        if (from >= 0 && to >= 0 && to != from) {
            onMove(from, to)
        }
        onSaveOrder()
    }

    fun onDragCancel() {
        isDragging = false
        draggingKey = null
        draggingBadge = null
        draggingIndex = -1
        dragPointerId = PointerId(-1)
        dropInsertBeforeIndex = -1
        draggingItemHeight = 0f
    }

    fun itemHeightForKey(key: String): Float? {
        return listState.layoutInfo.visibleItemsInfo
            .find { it.key == key }
            ?.size
            ?.toFloat()
    }

    companion object {
        data class DropTarget(val index: Int, val indicatorY: Float)

        /**
         * 根据手指 Y 计算插入下标；index == itemCount 表示插到末尾。
         * 以各项中线为界：上半 → 该项之前，下半 → 该项之后。
         */
        internal fun resolveInsertBeforeIndex(
            listState: LazyListState,
            fingerY: Float,
            itemCount: Int,
        ): DropTarget? {
            if (itemCount <= 0) {
                // 仅剩被拖项时，落点在列表顶部
                return DropTarget(0, 0f)
            }
            val items = listState.layoutInfo.visibleItemsInfo
            if (items.isEmpty()) return null

            for (item in items) {
                val top = item.offset.toFloat()
                val mid = top + item.size / 2f
                if (fingerY < mid) {
                    return DropTarget(item.index, top)
                }
            }

            val last = items.last()
            val bottom = last.offset + last.size.toFloat()
            val insertBefore = (last.index + 1).coerceAtMost(itemCount)
            return DropTarget(insertBefore, bottom)
        }
    }
}

@Composable
fun rememberMultiTouchReorderState(
    listState: LazyListState,
    onMove: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit,
): MultiTouchReorderState {
    val onMoveState = androidx.compose.runtime.rememberUpdatedState(onMove)
    val onSaveOrderState = androidx.compose.runtime.rememberUpdatedState(onSaveOrder)
    return remember(listState) {
        MultiTouchReorderState(
            listState = listState,
            onMove = { from, to -> onMoveState.value(from, to) },
            onSaveOrder = { onSaveOrderState.value() },
        )
    }
}
