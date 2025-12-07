package io.tatooinoyo.star.badge.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.tatooinoyo.star.badge.data.BadgeRepository


// 简单的管理界面组件
@Composable
fun BadgeManagerScreen() {
    // 监听数据
    val badgeList by BadgeRepository.badges.collectAsState()

    // 输入框状态
    var titleInput by remember { mutableStateOf("") }
    var remarkInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("徽章管理", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // 输入区域
        OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            label = { Text("标题 (如: 喝水)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = remarkInput,
            onValueChange = { remarkInput = it },
            label = { Text("备注 (如: 8杯/天)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (titleInput.isNotBlank()) {
                    BadgeRepository.addBadge(titleInput, remarkInput)
                    // 清空输入
                    titleInput = ""
                    remarkInput = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加徽章")
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // 列表展示区域
        Text("当前列表 (悬浮窗将同步显示):", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(badgeList) { badge ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = badge.title, style = MaterialTheme.typography.titleMedium)
                            if (badge.remark.isNotEmpty()) {
                                Text(
                                    text = badge.remark,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // 删除按钮
                        IconButton(onClick = { BadgeRepository.removeBadge(badge.id) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}