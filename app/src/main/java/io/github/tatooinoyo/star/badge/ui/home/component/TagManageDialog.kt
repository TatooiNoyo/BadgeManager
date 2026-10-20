package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.ui.component.FilterChipStyled
import io.github.tatooinoyo.star.badge.ui.icons.BadgeIcons
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrangeLight
import io.github.tatooinoyo.star.badge.ui.theme.SurfacePanel
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import io.github.tatooinoyo.star.badge.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManageDialog(
    allTags: List<String>,
    selectedTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val currentSelected = remember { mutableStateListOf(*selectedTags.toTypedArray()) }
    var newTagInput by remember { mutableStateOf("") }

    fun addTag() {
        val tag = newTagInput.trim()
        if (tag.isEmpty()) return
        if (tag !in currentSelected) currentSelected.add(tag)
        newTagInput = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfacePanel,
        shape = MaterialTheme.shapes.large,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(BrandOrangeLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(BadgeIcons.Label),
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.manage_tags),
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.tag_selected_count, currentSelected.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White)
                        .border(1.dp, BorderDefault, MaterialTheme.shapes.small)
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BasicTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                        decorationBox = { innerTextField ->
                            Box {
                                if (newTagInput.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.new_tag),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    Button(
                        onClick = ::addTag,
                        enabled = newTagInput.isNotBlank(),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) {
                        Text(stringResource(R.string.btn_add))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.select_tags),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                    )

                    val displayTags = (allTags + currentSelected).distinct().sorted()
                    if (displayTags.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color.White.copy(alpha = 0.72f),
                            shape = MaterialTheme.shapes.small,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderDefault),
                        ) {
                            Text(
                                text = stringResource(R.string.no_tags),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            displayTags.forEach { tag ->
                                val selected = tag in currentSelected
                                FilterChipStyled(
                                    label = tag,
                                    selected = selected,
                                    showCheck = false,
                                    onClick = {
                                        if (selected) currentSelected.remove(tag)
                                        else currentSelected.add(tag)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(currentSelected.toList())
                    onDismiss()
                },
                shape = MaterialTheme.shapes.small,
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview("TagManageDialog")
@Composable
private fun PreviewTagManageDialog() {
    TagManageDialog(
        allTags = listOf("常用", "季节", "联动"),
        selectedTags = listOf("常用"),
        onDismiss = {},
        onConfirm = {},
    )
}
