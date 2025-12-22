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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.navigation.NavRoutes
import io.github.tatooinoyo.star.badge.ui.home.component.BadgeFunctionArea
import io.github.tatooinoyo.star.badge.ui.home.component.BadgeReorderList

import io.github.tatooinoyo.star.badge.ui.home.component.TagFilterBar
import io.github.tatooinoyo.star.badge.ui.home.component.TagManageDialog
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import io.github.tatooinoyo.star.badge.ui.theme.PeachTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun BadgeManagerScreen(
    nfcPayload: String? = null,
    onNfcDataConsumed: () -> Unit = {},
    viewModel: BadgeManagerViewModel = viewModel(),
    badgeSyncViewModel: BadgeSyncViewModel = viewModel(),
    navController: NavController? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncState by badgeSyncViewModel.syncState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val listState = rememberLazyListState()

    LaunchedEffect(nfcPayload) {
        if (!nfcPayload.isNullOrEmpty()) {
            viewModel.onNfcPayloadReceived(nfcPayload)
            onNfcDataConsumed()
        }

        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is BadgeUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    // 自动录入成功后，清除输入框焦点
                    focusManager.clearFocus()
                }
            }
        }
    }

    if (uiState.editingBadge == null) {
        BadgeListContent(
            uiState = uiState,
            listState = listState,
            onInputTitleChange = { viewModel.updateAddInput(title = it) },
            onInputRemarkChange = { viewModel.updateAddInput(remark = it) },
            onInputLinkChange = { viewModel.updateAddInput(link = it) },
            onInputChannelChange = { viewModel.updateAddInput(channel = it) },
            onFastModeChange = viewModel::toggleFastMode,
            onAddClick = { viewModel.addBadge() },
            onItemClick = { badge -> viewModel.selectBadge(badge) },
            onToggleFunctionArea = { viewModel.toggleFunctionArea() },
            onExtractSkClick = { link -> viewModel.extractSkFromLink(link) },
            onTagsChange = { viewModel.updateAddInput(tags = it) },
            onTagSelected = { tag -> viewModel.selectTag(tag) },
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
            },
            // 导航相关
            onSettingsClick = { navController?.navigate(NavRoutes.Settings) },
            onAboutClick = { navController?.navigate(NavRoutes.About) }
        )
    } else {
        BadgeDetailContent(
            badge = uiState.editingBadge!!,
            title = uiState.detailTitle,
            remark = uiState.detailRemark,
            link = uiState.detailLink,
            channel = uiState.detailChannel,
            tags = uiState.detailTags,
            allTags = uiState.allTags,
            onTagsChange = { viewModel.updateDetailInput(tags = it) },
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
    listState: LazyListState,
    onInputTitleChange: (String) -> Unit,
    onInputRemarkChange: (String) -> Unit,
    onInputLinkChange: (String) -> Unit,
    onInputChannelChange: (BadgeChannel) -> Unit,
    onFastModeChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (Badge) -> Unit,
    onToggleFunctionArea: () -> Unit,
    onExtractSkClick: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onTagSelected: (String?) -> Unit = {},
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
    // 导航相关
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
//            .padding(16.dp)
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
            onFastModeChange = onFastModeChange,
            onAddClick = onAddClick,
            onToggleExpanded = onToggleFunctionArea,
            onExtractSkClick = onExtractSkClick,
            onTagsChange = onTagsChange,
            onStartSender = onStartSender,
            onStopSender = onStopSender,
            onStartReceiver = onStartReceiver,
            onStopReceiver = onStopReceiver,
            onImport = onImport,
            onExport = onExport,
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick
        )

        // 标签筛选栏
        // 放在列表上方
        TagFilterBar(
            allTags = uiState.allTags,
            selectedTag = uiState.selectedTag,
            onTagSelected = onTagSelected
        )

        // 2. 列表区域
        BadgeReorderList(
            badges = uiState.badges,
            onItemClick = onItemClick,
            onMove = onMove,
            onSaveOrder = onSaveOrder,
            listState = listState,
//            modifier = Modifier.weight(1f) // 占据剩余空间
        )


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
    tags: List<String>,
    allTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
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
        Text(
            stringResource(R.string.edit_badge_details),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 复用输入表单
        BadgeInputForm(
            title = title, onTitleChange = onTitleChange,
            remark = remark, onRemarkChange = onRemarkChange,
            link = link, onLinkChange = onLinkChange,
            channel = channel, onChannelChange = onChannelChange,
            allTags = allTags,
            selectedTags = tags,
            onTagsChange = onTagsChange,
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
            Text(stringResource(R.string.write_link_to_nfc_card))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮操作区
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 退出按钮
            OutlinedButton(onClick = onExitClick) {
                Text(stringResource(R.string.exit))
            }

            Row {
                // 删除按钮
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
                Spacer(modifier = Modifier.width(8.dp))
                // 更新按钮
                Button(
                    onClick = { showUpdateConfirm = true }
                ) {
                    Text(stringResource(R.string.save_update))
                }
            }
        }
    }

    // === 删除确认弹窗 ===
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.confirm_delete_message, badge.title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.confirm_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // === 更新确认弹窗 ===
    if (showUpdateConfirm) {
        AlertDialog(
            onDismissRequest = { showUpdateConfirm = false },
            title = { Text(stringResource(R.string.confirm_update)) },
            text = { Text(stringResource(R.string.confirm_update_message, badge.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showUpdateConfirm = false
                    onSaveClick()
                }) {
                    Text(stringResource(R.string.confirm_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // === NFC 写入等待弹窗 ===
    if (isWritingNfc) {
        AlertDialog(
            onDismissRequest = onCancelWriteNfcClick,
            title = { Text(stringResource(R.string.prepare_write_nfc)) },
            text = { Text(stringResource(R.string.prepare_write_nfc_message, link)) },
            confirmButton = {
                TextButton(onClick = onCancelWriteNfcClick) {
                    Text(stringResource(R.string.cancel))
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
    onExtractSkClick: (String) -> Unit, // 新增回调
    allTags: List<String> = emptyList(),
    selectedTags: List<String> = emptyList(),
    onTagsChange: (List<String>) -> Unit,
    isFastMode: Boolean = false
) {
    var channelMenuExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var lastLinkContent by remember { mutableStateOf("") }
    var showTagDialog by remember { mutableStateOf(false) }
    // 弹窗组件
    if (showTagDialog) {
        TagManageDialog(
            allTags = allTags, // 需确保 UiState 中有此字段
            selectedTags = selectedTags, // 需确保 UiState 中有此字段
            onDismiss = { showTagDialog = false },
            onConfirm = { newTags ->
                onTagsChange(newTags)
            }
        )
    }

    Column {
        OutlinedTextField(
            value = title, onValueChange = onTitleChange,
            label = {
                Text(
                    stringResource(R.string.title_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = remark, onValueChange = onRemarkChange,
            label = {
                Text(
                    stringResource(R.string.remark_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = link, onValueChange = onLinkChange,
            label = {
                Text(
                    stringResource(R.string.link_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    // 保持原有的逻辑：当点击获得焦点时触发
                    if (focusState.isFocused) {
                        // 仅在快速模式下执行
                        if (isFastMode) {
                            val clipboardContent = clipboardManager.getText()?.text
                            if (!clipboardContent.isNullOrBlank() &&
                                (clipboardContent.startsWith("http", ignoreCase = true) ||
                                        clipboardContent.startsWith("sky", ignoreCase = true))
                            ) {
                                // 如果内容不同，执行粘贴
                                if (lastLinkContent != clipboardContent) {
                                    lastLinkContent = clipboardContent
                                    onLinkChange(clipboardContent)
                                }
                            }
                        }
                    }
                },
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

            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                // 使用 Box 和一个不可编辑的 TextField 来模拟下拉框触发器
                // 注意：这里我们覆盖在 TextField 上加了一个点击区域
                Box(
                    modifier = Modifier
                        .clickable { channelMenuExpanded = true }
                ) {
                    OutlinedTextField(
                        value = channel.getLabel(LocalContext.current),
                        onValueChange = {},
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
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
                            text = { Text(option.getLabel(LocalContext.current)) },
                            onClick = {
                                onChannelChange(option)
                                channelMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 管理按钮
                IconButton(onClick = { showTagDialog = true }) {
                    Icon(
                        // 如果没有 Icons.Default.Label，请使用 Icons.Default.Sell 或导入扩展库
                        imageVector = Icons.AutoMirrored.Default.Label,
                        contentDescription = stringResource(R.string.manage_tags),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 2. 显示标签
                if (allTags.isEmpty()) {
                    Text(
                        text = "添加标签...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        items(selectedTags) { tag ->
                            InputChip(
                                selected = true,
                                onClick = { showTagDialog = true },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}