package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import io.github.tatooinoyo.star.badge.ui.component.BadgeIconContainer
import io.github.tatooinoyo.star.badge.ui.component.CategoryTag
import io.github.tatooinoyo.star.badge.ui.component.DragHandle as DragHandleIcon
import io.github.tatooinoyo.star.badge.ui.component.ServerTag
import io.github.tatooinoyo.star.badge.ui.theme.BadgeTokens
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val REORDER_PLACEMENT_MS = 250
private const val FLOAT_APPEAR_MS = 180
private const val FLOAT_SCALE = 0.88f
private const val FLOAT_ALPHA = 0.92f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeTagList(tags: List<String>) {
    if (tags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tags.forEach { tag ->
                CategoryTag(text = tag)
            }
        }
    }
}

@Composable
fun BadgeReorderList(
    badges: List<Badge>,
    onItemClick: (Badge) -> Unit,
    onMove: (String, String?) -> Boolean,
    onSaveOrder: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    isShareSelecting: Boolean = false,
    shareSelectedIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
) {
    val reorderState = rememberMultiTouchReorderState(listState, onMove, onSaveOrder)

    // 拖拽中仍保留列表项作为半透明影子；落点下标相对完整列表
    reorderState.updateItemKeys(badges.map { it.id })

    val density = LocalDensity.current
    val indicatorHalfHeightPx = with(density) { 1.5.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(listState, reorderState) {
                coroutineScope {
                    val scrollChannel = Channel<Float>(Channel.CONFLATED)
                    launch {
                        for (dy in scrollChannel) {
                            listState.scrollBy(-dy)
                            reorderState.onListScrolled()
                        }
                    }
                    try {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (!reorderState.isDragging) continue

                                val dragPointerId = reorderState.dragPointerId
                                var dragEnded = false

                                event.changes.forEach { change ->
                                    if (change.id == dragPointerId) {
                                        reorderState.updateDragFingerY(change.position.y)
                                        if (!change.pressed) {
                                            dragEnded = true
                                        }
                                        // 拖拽开始后由列表容器持有并消费主指针，避免手柄项
                                        // 滑出可见区被回收时中断手势，也避免误触标签横滑。
                                        change.consume()
                                    } else if (change.pressed) {
                                        val dy = change.positionChange().y
                                        if (dy != 0f) {
                                            scrollChannel.trySend(dy)
                                        }
                                        change.consume()
                                    }
                                }

                                if (dragEnded) {
                                    reorderState.onDragEnd(dragPointerId)
                                }
                            }
                        }
                    } finally {
                        // 容器真正退出或手势监听被重建时才取消仍在进行的拖拽。
                        reorderState.onDragCancel()
                        scrollChannel.close()
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            userScrollEnabled = !reorderState.isDragging,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { reorderState.listCoordinates = it }
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (badges.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.badge_list_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 8.dp),
                    )
                }
            }
            items(badges, key = { badge -> badge.id }) { badge ->
                val isGhost = reorderState.isDragging && badge.id == reorderState.draggingKey
                BadgeListCard(
                    badge = badge,
                    elevated = false,
                    isGhost = isGhost,
                    onClick = { onItemClick(badge) },
                    clickEnabled = (!reorderState.isDragging || isShareSelecting) && !isGhost,
                    isShareSelecting = isShareSelecting,
                    isShareSelected = badge.id in shareSelectedIds,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = tween(REORDER_PLACEMENT_MS),
                    ),
                    dragHandle = if (isShareSelecting) {
                        {
                            Checkbox(
                                checked = badge.id in shareSelectedIds,
                                onCheckedChange = { onItemClick(badge) },
                            )
                        }
                    } else {
                        {
                            DragHandle(
                                badge = badge,
                                reorderState = reorderState,
                            )
                        }
                    },
                )
            }
        }

        // 落点横杆（高于悬浮层，避免被挡住）
        val moveTo = reorderState.resolvedMoveToIndex()
        val showDropIndicator = reorderState.isDragging &&
            reorderState.dropInsertBeforeIndex >= 0 &&
            moveTo != reorderState.currentDraggingIndex()
        if (showDropIndicator) {
            Box(
                modifier = Modifier
                    .zIndex(25f)
                    .padding(horizontal = 16.dp)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (reorderState.dropIndicatorY - indicatorHalfHeightPx).roundToInt()
                        )
                    }
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }

        // 悬浮被拖项：出现时缩小/透明过渡，减少遮挡横杆
        val draggingBadge = reorderState.draggingBadge
        if (reorderState.isDragging && draggingBadge != null) {
            FloatingDragBadge(
                badge = draggingBadge,
                fingerY = reorderState.dragFingerY,
                itemHeight = reorderState.draggingItemHeight,
            )
        }
    }
}

@Composable
private fun FloatingDragBadge(
    badge: Badge,
    fingerY: Float,
    itemHeight: Float,
) {
    val floatScale = remember { Animatable(1f) }
    val floatAlpha = remember { Animatable(1f) }
    LaunchedEffect(badge.id) {
        floatScale.snapTo(1f)
        floatAlpha.snapTo(1f)
        launch { floatScale.animateTo(FLOAT_SCALE, tween(FLOAT_APPEAR_MS)) }
        launch { floatAlpha.animateTo(FLOAT_ALPHA, tween(FLOAT_APPEAR_MS)) }
    }
    val halfH = itemHeight * floatScale.value / 2f
    Box(
        modifier = Modifier
            .zIndex(20f)
            .padding(horizontal = 24.dp)
            .offset {
                IntOffset(
                    x = 0,
                    y = (fingerY - halfH).roundToInt()
                )
            }
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = floatScale.value
                scaleY = floatScale.value
                alpha = floatAlpha.value
            }
    ) {
        BadgeListCard(
            badge = badge,
            elevated = true,
            onClick = {},
            clickEnabled = false,
            dragHandle = {
                DragHandleIcon()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BadgeListCard(
    badge: Badge,
    elevated: Boolean,
    onClick: () -> Unit,
    clickEnabled: Boolean,
    isShareSelecting: Boolean = false,
    isShareSelected: Boolean = false,
    isGhost: Boolean = false,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    val cardColor = when {
        elevated -> MaterialTheme.colorScheme.surfaceContainerLowest
        isShareSelecting && isShareSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isGhost -> BadgeTokens.badgeCardBackground.copy(alpha = 0.35f)
        else -> BadgeTokens.badgeCardBackground
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .graphicsLayer { if (isGhost) alpha = 0.4f }
            .clip(MaterialTheme.shapes.medium)
            .background(cardColor)
            .border(1.dp, BorderDefault, MaterialTheme.shapes.medium)
            .clickable(enabled = clickEnabled, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BadgeIconContainer()
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ServerTag(text = badge.getChannelLabel(LocalContext.current))
                    badge.tags.forEach { tag ->
                        CategoryTag(text = tag)
                    }
                }
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = badge.remark.ifEmpty { " " },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
        if (isShareSelecting) {
            Checkbox(
                checked = isShareSelected,
                onCheckedChange = null,
            )
        } else {
            dragHandle()
        }
    }
}

@Composable
private fun DragHandle(
    badge: Badge,
    reorderState: MultiTouchReorderState,
) {
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentBadge by rememberUpdatedState(badge)

    DragHandleIcon(
        modifier = Modifier
            .onGloballyPositioned { handleCoordinates = it }
            .pointerInput(badge.id, reorderState) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val coords = handleCoordinates
                    val listCoords = reorderState.listCoordinates
                    val gestureBadge = currentBadge
                    if (coords == null ||
                        listCoords == null ||
                        !coords.isAttached ||
                        !listCoords.isAttached
                    ) {
                        return@awaitEachGesture
                    }

                    val startFingerY = listCoords.localPositionOf(coords, down.position).y
                    val itemHeight = reorderState.itemHeightForKey(gestureBadge.id)
                        ?: 72.dp.toPx()
                    val started = reorderState.onDragStart(
                        badge = gestureBadge,
                        fingerY = startFingerY,
                        pointerId = down.id,
                        itemHeight = itemHeight,
                    )
                    if (!started) return@awaitEachGesture
                    down.consume()
                }
            }
    )
}
