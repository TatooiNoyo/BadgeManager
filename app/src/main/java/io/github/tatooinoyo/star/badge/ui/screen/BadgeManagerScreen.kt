package io.github.tatooinoyo.star.badge.ui.screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorder
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable

@Composable
fun BadgeManagerScreen(
    nfcPayload: String? = null,
    onNfcDataConsumed: () -> Unit = {},
    viewModel: BadgeManagerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(nfcPayload) {
        if (!nfcPayload.isNullOrEmpty()) {
            viewModel.onNfcPayloadReceived(nfcPayload)
            onNfcDataConsumed()
        }
    }

    if (uiState.editingBadge == null) {
        BadgeListContent(
            uiState = uiState,
            onInputTitleChange = { viewModel.updateAddInput(title = it) },
            onInputRemarkChange = { viewModel.updateAddInput(remark = it) },
            onInputLinkChange = { viewModel.updateAddInput(link = it) },
            onInputChannelChange = { viewModel.updateAddInput(channel = it) },
            onAddClick = { viewModel.addBadge() },
            onItemClick = { badge -> viewModel.selectBadge(badge) },
            onExtractSkClick = { link -> viewModel.extractSkFromLink(link) },
            onMove = { from, to -> viewModel.moveBadge(from, to) },
            onSaveOrder = { viewModel.saveOrder() }
        )
    } else {
        BadgeDetailContent(
            badge = uiState.editingBadge!!,
            title = uiState.detailTitle,
            remark = uiState.detailRemark,
            link = uiState.detailLink,
            channel = uiState.detailChannel,
            isWritingNfc = uiState.isWritingNfc,
            onTitleChange = { viewModel.updateDetailInput(title = it) },
            onRemarkChange = { viewModel.updateDetailInput(remark = it) },
            onLinkChange = { viewModel.updateDetailInput(link = it) },
            onChannelChange = { viewModel.updateDetailInput(channel = it) },
            onWriteNfcClick = { viewModel.startWritingNfc() },
            onCancelWriteNfcClick = { viewModel.cancelWritingNfc() },
            onSaveClick = { viewModel.saveBadgeUpdate() },
            onDeleteClick = { viewModel.deleteBadge() },
            onExitClick = { viewModel.exitEditMode() },
            onExtractSkClick = { link -> viewModel.extractSkFromLink(link) }
        )
    }

    uiState.extractedSk?.let { sk ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissSkDialog() },
            title = { Text("提取到的 SK 编码") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sk, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(sk))
                        Toast.makeText(context, "已复制到剪切板", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("复制")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSkDialog() }) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
fun BadgeListContent(
    uiState: BadgeUiState,
    onInputTitleChange: (String) -> Unit,
    onInputRemarkChange: (String) -> Unit,
    onInputLinkChange: (String) -> Unit,
    onInputChannelChange: (BadgeChannel) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (Badge) -> Unit,
    onExtractSkClick: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onSaveOrder: () -> Unit
) {
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to -> onMove(from.index, to.index) },
        onDragEnd = { _, _ -> onSaveOrder() }
    )
    var isAddAreaExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {

        // 使用 AnimatedVisibility 包裹添加表单
        AnimatedVisibility(visible = isAddAreaExpanded) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                BadgeInputForm(
                    title = uiState.addTitle, onTitleChange = onInputTitleChange,
                    remark = uiState.addRemark, onRemarkChange = onInputRemarkChange,
                    link = uiState.addLink, onLinkChange = onInputLinkChange,
                    channel = uiState.addChannel, onChannelChange = onInputChannelChange,
                    onExtractSkClick = onExtractSkClick
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("添加徽章")
                }
            }
        }

        // 可点击的折叠栏，用于折叠/展开
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAddAreaExpanded = !isAddAreaExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isAddAreaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isAddAreaExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Text("点击卡片查看详情,拖拽卡片右侧排序:", style = MaterialTheme.typography.titleMedium)

        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier.reorderable(reorderableState),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.badges, { it.id }) { badge ->
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
}

// === 视图 2: 详情编辑页面 ===
@Composable
fun BadgeDetailContent(
    badge: Badge,
    title: String,
    remark: String,
    link: String,
    channel: BadgeChannel,
    isWritingNfc: Boolean,
    onTitleChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onChannelChange: (BadgeChannel) -> Unit,
    onWriteNfcClick: () -> Unit,
    onCancelWriteNfcClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onExitClick: () -> Unit,
    onExtractSkClick: (String) -> Unit
) {
    // 弹窗控制状态
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showUpdateConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Text("编辑徽章详情", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // 复用输入表单
        BadgeInputForm(
            title = title, onTitleChange = onTitleChange,
            remark = remark, onRemarkChange = onRemarkChange,
            link = link, onLinkChange = onLinkChange,
            channel = channel, onChannelChange = onChannelChange,
            onExtractSkClick = onExtractSkClick
        )

        Spacer(modifier = Modifier.weight(1f))

        // 新增：写入 NFC 按钮
        Button(
            onClick = onWriteNfcClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("写入链接到 NFC 卡片")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮操作区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 退出按钮
            OutlinedButton(onClick = onExitClick) {
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
                        showDeleteConfirm = false
                        onDeleteClick()
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
                    showUpdateConfirm = false
                    onSaveClick()
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

    // === NFC 写入等待弹窗 ===
    if (isWritingNfc) {
        AlertDialog(
            onDismissRequest = onCancelWriteNfcClick,
            title = { Text("准备写入 NFC") },
            text = { Text("请将 NFC 卡片紧贴手机背面以写入数据。\n目标链接: $link") },
            confirmButton = {
                TextButton(onClick = onCancelWriteNfcClick) {
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
    channel: BadgeChannel, onChannelChange: (BadgeChannel) -> Unit,
    onExtractSkClick: (String) -> Unit // 新增回调
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
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { onExtractSkClick(link) }) {
                    Text("SK")
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("渠道类型：", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.width(8.dp))

            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                // 使用 Box 和一个不可编辑的 TextField 来模拟下拉框触发器
                // 注意：这里我们覆盖在 TextField 上加了一个点击区域
                Box(
                    modifier = Modifier
                        .clickable { channelMenuExpanded = true }
                ) {
                    OutlinedTextField(
                        value = channel.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        },
                        modifier = Modifier.width(150.dp),
                        enabled = false, // 禁用自带输入，完全靠点击触发
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledContainerColor = Color.Transparent,
                            disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                DropdownMenu(
                    expanded = channelMenuExpanded,
                    onDismissRequest = { channelMenuExpanded = false }
                ) {
                    BadgeChannel.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                onChannelChange(option)
                                channelMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}