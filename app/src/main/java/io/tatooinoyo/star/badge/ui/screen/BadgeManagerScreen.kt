package io.tatooinoyo.star.badge.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.tatooinoyo.star.badge.data.Badge
import io.tatooinoyo.star.badge.data.BadgeChannel
import io.tatooinoyo.star.badge.data.BadgeRepository

@Composable
fun BadgeManagerScreen(
    nfcPayload: String? = null,
    onNfcDataConsumed: () -> Unit = {}
) {
    // 页面状态：当前正在编辑的徽章。如果是 null，则显示列表；否则显示详情页
    var editingBadge by remember { mutableStateOf<Badge?>(null) }

    // 监听 NFC 数据变化
    // 当 nfcPayload 变成非空字符串时，我们判断当前是在哪里
    androidx.compose.runtime.LaunchedEffect(nfcPayload) {
        if (!nfcPayload.isNullOrEmpty()) {
            // 如果还没打开详情页，我们无法填充到详情页，
            // 这里假设默认行为：如果有 NFC 数据进来，且当前处于列表页，
            // 我们可能只更新列表页的 "添加区域" 的 linkInput 状态。
            // 具体逻辑在下面传递给 BadgeListContent 或 BadgeDetailContent 处理
        }
    }

    // 根据状态切换视图
    if (editingBadge == null) {
        BadgeListContent(
            onItemClick = { badge -> editingBadge = badge },
            nfcPayload = nfcPayload, // 传下去
            onNfcDataConsumed = onNfcDataConsumed
        )
    } else {
        BadgeDetailContent(
            badge = editingBadge!!,
            onExit = { editingBadge = null },
            nfcPayload = nfcPayload, // 传下去
            onNfcDataConsumed = onNfcDataConsumed
        )
    }
}

// === 视图 1: 列表与添加页面 (主页) ===
@Composable
fun BadgeListContent(
    onItemClick: (Badge) -> Unit,
    nfcPayload: String?,
    onNfcDataConsumed: () -> Unit
) {
    // 监听数据
    val badgeList by BadgeRepository.badges.collectAsState()

    // 添加模式下的输入状态
    var titleInput by remember { mutableStateOf("") }
    var remarkInput by remember { mutableStateOf("") }
    var linkInput by remember { mutableStateOf("") }
    var selectedChannel by remember { mutableStateOf(BadgeChannel.HUAWEI) }


    // 监听 NFC 数据并填充
    androidx.compose.runtime.LaunchedEffect(nfcPayload) {
        if (!nfcPayload.isNullOrEmpty()) {
            linkInput = nfcPayload // 自动填充到 Link 框
            onNfcDataConsumed()    // 通知上层清空数据，防止重组时再次覆盖用户的手动修改
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("徽章管理", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        // === 添加区域 (复用之前的逻辑) ===
        BadgeInputForm(
            title = titleInput, onTitleChange = { titleInput = it },
            remark = remarkInput, onRemarkChange = { remarkInput = it },
            link = linkInput, onLinkChange = { linkInput = it },
            channel = selectedChannel, onChannelChange = { selectedChannel = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (titleInput.isNotBlank()) {
                    BadgeRepository.addBadge(titleInput, remarkInput, linkInput, selectedChannel)
                    // 清空输入
                    titleInput = ""
                    remarkInput = ""
                    linkInput = ""
                    selectedChannel = BadgeChannel.HUAWEI
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("添加徽章")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // === 列表区域 ===
        Text("点击列表项查看详情/编辑:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(badgeList) { badge ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onItemClick(badge) }, // 点击进入详情
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
                            // 标题 + 渠道
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
                            }
                        }
                        // 注意：这里移除了删除按钮
                    }
                }
            }
        }
    }
}

// === 视图 2: 详情编辑页面 ===
@Composable
fun BadgeDetailContent(badge: Badge,
                       onExit: () -> Unit,
                       nfcPayload: String?,
                       onNfcDataConsumed: () -> Unit) {
    // 编辑状态，初始化为传入的徽章数据
    var title by remember { mutableStateOf(badge.title) }
    var remark by remember { mutableStateOf(badge.remark) }
    var link by remember { mutableStateOf(badge.link) }
    var channel by remember { mutableStateOf(badge.channel) }

    // 如果在编辑模式下触碰 NFC，更新当前编辑的链接
    androidx.compose.runtime.LaunchedEffect(nfcPayload) {
        if (!nfcPayload.isNullOrEmpty()) {
            link = nfcPayload
            onNfcDataConsumed()
        }
    }

    // 弹窗控制状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("编辑徽章详情", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // 复用输入表单
        BadgeInputForm(
            title = title, onTitleChange = { title = it },
            remark = remark, onRemarkChange = { remark = it },
            link = link, onLinkChange = { link = it },
            channel = channel, onChannelChange = { channel = it }
        )

        Spacer(modifier = Modifier.weight(1f)) // 占位，把按钮推到底部

        // 按钮操作区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 退出按钮
            OutlinedButton(onClick = onExit) {
                Text("退出")
            }

            Row {
                // 删除按钮
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除")
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 更新按钮
                Button(
                    onClick = { showUpdateConfirm = true }
                ) {
                    Text("保存更新")
                }
            }
        }
    }

    // === 删除确认弹窗 ===
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("确定要删除徽章“${badge.title}”吗？此操作无法撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        BadgeRepository.removeBadge(badge.id)
                        showDeleteConfirm = false
                        onExit() // 删除后退出详情页
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // === 更新确认弹窗 ===
    if (showUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = false },
            title = { Text("确认更新") },
            text = { Text("确定要保存对“${badge.title}”的修改吗？") },
            confirmButton = {
                TextButton(onClick = {
                    // 调用 Repository 的更新方法
                    BadgeRepository.updateBadge(badge.id, title, remark, link, channel)

                    showUpdateConfirm = false
                    onExit()
                }) {
                    Text("确认保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// === 提取出来的通用输入表单组件 ===
@Composable
fun BadgeInputForm(
    title: String, onTitleChange: (String) -> Unit,
    remark: String, onRemarkChange: (String) -> Unit,
    link: String, onLinkChange: (String) -> Unit,
    channel: BadgeChannel, onChannelChange: (BadgeChannel) -> Unit
) {
    var channelMenuExpanded by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = { Text("标题 (如: 小不点)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = remark, onValueChange = onRemarkChange,
            label = { Text("备注 (如: 15分钟冷却，持续20分钟)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = link, onValueChange = onLinkChange,
            label = { Text("链接 (如: https://...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("渠道类型：", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                OutlinedTextField(
                    value = channel.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { channelMenuExpanded = true },
                    enabled = false,
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = Color.Transparent,
                        disabledIndicatorColor = MaterialTheme.colorScheme.outline
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { channelMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = channelMenuExpanded,
                    onDismissRequest = { channelMenuExpanded = false }
                ) {
                    BadgeChannel.values().forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.label) },
                            onClick = {
                                onChannelChange(c)
                                channelMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
