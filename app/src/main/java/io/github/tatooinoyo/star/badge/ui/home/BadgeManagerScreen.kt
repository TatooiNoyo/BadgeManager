package io.github.tatooinoyo.star.badge.ui.home

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.ui.home.component.BadgeFunctionArea
import io.github.tatooinoyo.star.badge.ui.home.component.BadgeReorderList
import io.github.tatooinoyo.star.badge.ui.home.component.HelpInfoDialog
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import io.github.tatooinoyo.star.badge.ui.theme.PeachTheme

@Composable
fun BadgeManagerScreen(
    nfcPayload: String? = null,
    onNfcDataConsumed: () -> Unit = {},
    viewModel: BadgeManagerViewModel = viewModel(),
    badgeSyncViewModel: BadgeSyncViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by badgeSyncViewModel.syncState.collectAsState()
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
            onSaveOrder = { viewModel.saveOrder() },
            // === 传递同步参数 ===
            syncState = syncState,
            onStartSender = { badgeSyncViewModel.startSenderMode() },
            onStopSender = { badgeSyncViewModel.stopSenderMode() },
            onStartReceiver = { code -> badgeSyncViewModel.startReceiverMode(code) },
            onStopReceiver = { badgeSyncViewModel.stopReceiverMode() },
            onImport = { ctx, uri, onResult ->
                viewModel.importBadgesFromUri(ctx, uri, onResult)
            },
            onExport = { ctx, uri, onResult ->
                viewModel.exportBadgesToUri(ctx, uri, onResult)
            }
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
            title = { Text(stringResource(R.string.dialog_sk_title)) },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(sk, modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(sk))
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_copy_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Text(stringResource(R.string.btn_copy))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissSkDialog() }) {
                    Text(stringResource(R.string.btn_confirm))
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
    onSaveOrder: () -> Unit,
    // 同步相关参数 ===
    syncState: SyncState,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onStartReceiver: (String) -> Unit,
    onStopReceiver: () -> Unit,
    onImport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onExport: (Context, Uri, (Boolean) -> Unit) -> Unit,
) {

    // 帮助弹窗的状态和 URI 处理器
    var showHelpDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
            .padding(16.dp)
            .safeDrawingPadding()
    ) {
        // 1. 顶部功能区 (录入、备份、同步)
        BadgeFunctionArea(
            uiState = uiState,
            syncState = syncState,
            onInputTitleChange = onInputTitleChange,
            onInputRemarkChange = onInputRemarkChange,
            onInputLinkChange = onInputLinkChange,
            onInputChannelChange = onInputChannelChange,
            onAddClick = onAddClick,
            onExtractSkClick = onExtractSkClick,
            onStartSender = onStartSender,
            onStopSender = onStopSender,
            onStartReceiver = onStartReceiver,
            onStopReceiver = onStopReceiver,
            onImport = onImport,
            onExport = onExport,
            onHelpClick = { showHelpDialog = true }
        )


        // 2. 列表区域
        BadgeReorderList(
            badges = uiState.badges,
            onItemClick = onItemClick,
            onMove = onMove,
            onSaveOrder = onSaveOrder,
//            modifier = Modifier.weight(1f) // 占据剩余空间
        )


        if (showHelpDialog) {
            HelpInfoDialog(onDismiss = { showHelpDialog = false })
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

    // 拦截系统返回手势/按键
    // 当此 Composable 显示时，按返回键会触发 onExitClick，而不是直接退出 App
    BackHandler {
        onExitClick()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
            .padding(16.dp)
            .safeDrawingPadding()
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