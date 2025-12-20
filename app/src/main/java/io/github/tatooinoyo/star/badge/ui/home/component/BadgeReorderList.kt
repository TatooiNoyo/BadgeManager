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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.data.Badge
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

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
) {

    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> onMove(from.index, to.index) },
        onDragEnd = { _, _ -> onSaveOrder() },
        listState = listState
    )

    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier
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
                                        badge.channel.label,
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
                            modifier = Modifier.detectReorder(reorderableState) // 修正了这里
                        )
                    }
                }
            }
        }
    }
}