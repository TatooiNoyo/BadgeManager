package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManageDialog(
    allTags: List<String>,          // 系统中所有已存在的标签
    selectedTags: List<String>,     // 当前徽章已选中的标签
    onDismiss: () -> Unit,          // 关闭弹窗
    onConfirm: (List<String>) -> Unit // 确认选择
) {
    // 临时状态，用于在弹窗内操作，点击确定才回传
    val currentSelected = remember { mutableStateListOf(*selectedTags.toTypedArray()) }
    var newTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理标签") },
        text = {
            Column {
                // 1. 输入新标签区域
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        label = { Text("新建标签") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTagInput.isNotBlank()) {
                                val tag = newTagInput.trim()
                                // 新增并默认选中
                                if (!currentSelected.contains(tag)) {
                                    currentSelected.add(tag)
                                }
                                newTagInput = ""
                            }
                        },
                        enabled = newTagInput.isNotBlank()
                    ) {
                        Text("添加")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("选择标签:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // 2. 现有标签选择区域 (混合了系统已有标签和刚才新增的标签)
                // 这里的集合应该是 allTags + currentSelected (去重)，确保新加的也能看到
                val displayTags = (allTags + currentSelected).distinct().sorted()

                if (displayTags.isEmpty()) {
                    Text("暂无标签", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        displayTags.forEach { tag ->
                            val isSelected = currentSelected.contains(tag)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) {
                                        currentSelected.remove(tag)
                                    } else {
                                        currentSelected.add(tag)
                                    }
                                },
                                label = { Text(tag) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.CheckCircle, null,
                                            Modifier.size(16.dp)
                                        )
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(currentSelected.toList())
                    onDismiss()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
@Preview("TagManageDialog")
fun PreviewTagManageDialog(){
    TagManageDialog(listOf(), listOf(), {}, {})
}
