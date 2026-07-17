package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.data.Badge
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

private const val FUNCTION_AREA_SCROLL_THRESHOLD = 80f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeTagList(tags: List<String>) {
    if (tags.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp) // 紧凑排列
        ) {
            tags.forEach { tag ->
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = Green.copy(alpha = 0.8f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp) // 极窄内边距
                    )
                }
            }
        }
    }
}


@Composable
fun BadgeReorderList(
    badges: List<Badge>,
    onItemClick: (Badge) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    isFunctionAreaExpanded: Boolean = true,
    onSetFunctionAreaExpanded: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isExpandedState = rememberUpdatedState(isFunctionAreaExpanded)
    val onSetExpandedState = rememberUpdatedState(onSetFunctionAreaExpanded)

    val nestedScrollConnection = remember(listState) {
        var collapseDrag = 0f
        var expandDrag = 0f
        // 本次手势已触发折叠/展开后，剩余滑动继续消费，等松手后再允许翻列表
        var lockListUntilGestureEnd = false

        fun resetGestureTracking() {
            collapseDrag = 0f
            expandDrag = 0f
            lockListUntilGestureEnd = false
        }

        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) {
                    return Offset.Zero
                }
                // 本手势已用于切换功能区：继续吃掉滚动，不带动列表
                if (lockListUntilGestureEnd) {
                    return if (available.y != 0f) Offset(0f, available.y) else Offset.Zero
                }
                // 功能区展开时，上滑只用于折叠
                if (isExpandedState.value && available.y < 0f) {
                    collapseDrag += -available.y
                    if (collapseDrag >= FUNCTION_AREA_SCROLL_THRESHOLD) {
                        onSetExpandedState.value(false)
                        lockListUntilGestureEnd = true
                        collapseDrag = 0f
                    }
                    return Offset(0f, available.y)
                }
                collapseDrag = 0f
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source != NestedScrollSource.Drag) {
                    return Offset.Zero
                }
                if (lockListUntilGestureEnd) {
                    return if (available.y != 0f) Offset(0f, available.y) else Offset.Zero
                }
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                // 到顶且功能区收起时，下拉只用于展开
                if (atTop && !isExpandedState.value && available.y > 0f) {
                    expandDrag += available.y
                    if (expandDrag >= FUNCTION_AREA_SCROLL_THRESHOLD) {
                        onSetExpandedState.value(true)
                        lockListUntilGestureEnd = true
                        expandDrag = 0f
                    }
                    return Offset(0f, available.y)
                }
                expandDrag = 0f
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                resetGestureTracking()
                return Velocity.Zero
            }
        }
    }

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> onMove(from.index, to.index) },
        onDragEnd = { _, _ -> onSaveOrder() },
        listState = listState
    )

    LazyColumn(
        state = reorderableState.listState,
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .reorderable(reorderableState)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(badges, { it.id }) { badge ->
            ReorderableItem(reorderableState, key = badge.id) { isDragging ->
                val elevation = if (isDragging) 8.dp else 2.dp
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onItemClick(badge) },
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = badge.title,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(4.dp)
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        badge.channel.getLabel(LocalContext.current),
                                        modifier = Modifier.padding(4.dp),
                                        color = White,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                                BadgeTagList(tags = badge.tags)
                            }
                            if (badge.remark.isNotEmpty()) {
                                Text(
                                    text = badge.remark,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                Text(text = " ", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drag to reorder",
                            modifier = Modifier.detectReorder(reorderableState)
                        )
                    }
                }
            }
        }
    }
}
