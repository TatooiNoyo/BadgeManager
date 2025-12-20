package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.data.Badge
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

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
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                SuggestionChip(
                                    onClick = { },
                                    label = {
                                        Text(
                                            badge.channel.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.height(24.dp)
                                )
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