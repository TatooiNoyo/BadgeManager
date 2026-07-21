package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.tatooinoyo.star.badge.data.Badge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FUNCTION_AREA_SCROLL_THRESHOLD = 80f
private const val REORDER_PLACEMENT_MS = 250
private const val FLOAT_APPEAR_MS = 180
private const val INDICATOR_MOVE_MS = 120
private const val FLOAT_SCALE = 0.88f
private const val FLOAT_ALPHA = 0.92f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BadgeTagList(tags: List<String>) {
    if (tags.isNotEmpty()) {
        FlowRow(
            modifier = Modifier.padding(start = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
    val reorderState = rememberMultiTouchReorderState(listState, onMove, onSaveOrder)
    val isDraggingState = rememberUpdatedState(reorderState.isDragging)

    // 拖拽中从列表暂隐被拖项，下方自动补位；落点下标相对此展示列表
    val displayBadges = remember(badges, reorderState.draggingKey, reorderState.isDragging) {
        val key = reorderState.draggingKey
        if (reorderState.isDragging && key != null) {
            badges.filter { it.id != key }
        } else {
            badges
        }
    }
    reorderState.itemCount = displayBadges.size

    val density = LocalDensity.current
    val indicatorHalfHeightPx = with(density) { 1.5.dp.toPx() }

    val nestedScrollConnection = remember(listState) {
        var collapseDrag = 0f
        var expandDrag = 0f
        var lockListUntilGestureEnd = false

        fun resetGestureTracking() {
            collapseDrag = 0f
            expandDrag = 0f
            lockListUntilGestureEnd = false
        }

        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isDraggingState.value || source != NestedScrollSource.Drag) {
                    return Offset.Zero
                }
                if (lockListUntilGestureEnd) {
                    return if (available.y != 0f) Offset(0f, available.y) else Offset.Zero
                }
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
                if (isDraggingState.value || source != NestedScrollSource.Drag) {
                    return Offset.Zero
                }
                if (lockListUntilGestureEnd) {
                    return if (available.y != 0f) Offset(0f, available.y) else Offset.Zero
                }
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
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
                                    } else if (change.pressed) {
                                        val dy = change.positionChange().y
                                        if (dy != 0f) {
                                            scrollChannel.trySend(dy)
                                        }
                                        change.consume()
                                    }
                                }

                                if (dragEnded) {
                                    reorderState.onDragEnd()
                                }
                            }
                        }
                    } finally {
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
                .nestedScroll(nestedScrollConnection)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            itemsIndexed(displayBadges, key = { _, badge -> badge.id }) { _, badge ->
                BadgeListCard(
                    badge = badge,
                    elevated = false,
                    onClick = { onItemClick(badge) },
                    clickEnabled = !reorderState.isDragging,
                    modifier = Modifier.animateItem(
                        fadeInSpec = null,
                        fadeOutSpec = null,
                        placementSpec = tween(REORDER_PLACEMENT_MS),
                    ),
                    dragHandle = {
                        val fullIndex = badges.indexOfFirst { it.id == badge.id }
                        DragHandle(
                            badge = badge,
                            index = fullIndex,
                            reorderState = reorderState,
                        )
                    },
                )
            }
        }

        // 落点横杆（高于悬浮层，避免被挡住）
        val showDropIndicator = reorderState.isDragging &&
            reorderState.dropInsertBeforeIndex >= 0 &&
            reorderState.dropInsertBeforeIndex != reorderState.draggingIndex
        val animatedIndicatorY by animateFloatAsState(
            targetValue = reorderState.dropIndicatorY,
            animationSpec = tween(INDICATOR_MOVE_MS),
            label = "dropIndicatorY",
        )
        if (showDropIndicator) {
            Box(
                modifier = Modifier
                    .zIndex(25f)
                    .padding(horizontal = 16.dp)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (animatedIndicatorY - indicatorHalfHeightPx).roundToInt()
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
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                )
            },
        )
    }
}

@Composable
private fun BadgeListCard(
    badge: Badge,
    elevated: Boolean,
    onClick: () -> Unit,
    clickEnabled: Boolean,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = clickEnabled, onClick = onClick),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (elevated) 8.dp else 2.dp
        ),
        colors = if (elevated) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            CardDefaults.cardColors()
        },
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
            dragHandle()
        }
    }
}

@Composable
private fun DragHandle(
    badge: Badge,
    index: Int,
    reorderState: MultiTouchReorderState,
) {
    var handleCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val density = LocalDensity.current

    Icon(
        imageVector = Icons.Default.Menu,
        contentDescription = "Drag to reorder",
        modifier = Modifier
            .onGloballyPositioned { handleCoordinates = it }
            .pointerInput(badge.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val coords = handleCoordinates
                    val listCoords = reorderState.listCoordinates
                    if (index < 0 ||
                        coords == null ||
                        listCoords == null ||
                        !coords.isAttached ||
                        !listCoords.isAttached
                    ) {
                        return@awaitEachGesture
                    }

                    val startFingerY = listCoords.localPositionOf(coords, down.position).y
                    val itemHeight = reorderState.itemHeightForKey(badge.id)
                        ?: with(density) { 72.dp.toPx() }
                    down.consume()
                    reorderState.onDragStart(
                        badge = badge,
                        index = index,
                        fingerY = startFingerY,
                        pointerId = down.id,
                        itemHeight = itemHeight,
                    )

                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val dragChange = event.changes.find { it.id == down.id } ?: continue
                            if (!dragChange.pressed) {
                                reorderState.onDragEnd()
                                break
                            }
                            dragChange.consume()
                        }
                    } catch (e: CancellationException) {
                        throw e
                    }
                }
            }
    )
}
