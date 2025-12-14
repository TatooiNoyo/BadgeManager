package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TagFilterBar(
    allTags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit
) {
    // 如果没有任何标签，则不显示筛选栏，节省空间
    if (allTags.isEmpty()) return

    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // "全部" 选项
        item {
            androidx.compose.material3.FilterChip(
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
                label = { Text("全部") },
                leadingIcon = if (selectedTag == null) {
                    {
                        Icon(
                            Icons.Default.Check, contentDescription = null,
                            Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }

        // 各个标签选项
        items(allTags) { tag ->
            val isSelected = tag == selectedTag
            androidx.compose.material3.FilterChip(
                selected = isSelected,
                onClick = {
                    // 如果点击已选中的标签，则取消选择（回到全部）
                    onTagSelected(if (isSelected) null else tag)
                },
                label = { Text(tag) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check, contentDescription = null,
                            Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}
