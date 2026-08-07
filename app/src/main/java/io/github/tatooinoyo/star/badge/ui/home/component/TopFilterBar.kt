package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.ui.component.FilterChipStyled

@Composable
fun TagFilterBar(
    allTags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
) {
    if (allTags.isEmpty()) return

    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 0.dp,
            vertical = 0.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        item {
            FilterChipStyled(
                label = stringResource(R.string.tag_all),
                selected = selectedTag == null,
                onClick = { onTagSelected(null) },
            )
        }

        items(allTags) { tag ->
            val isSelected = tag == selectedTag
            FilterChipStyled(
                label = tag,
                selected = isSelected,
                onClick = {
                    onTagSelected(if (isSelected) null else tag)
                },
            )
        }
    }
}
