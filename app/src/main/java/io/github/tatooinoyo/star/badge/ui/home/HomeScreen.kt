package io.github.tatooinoyo.star.badge.ui.home

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannelTarget
import io.github.tatooinoyo.star.badge.ui.about.UpdateCheckDialog
import io.github.tatooinoyo.star.badge.ui.component.BadgeContentCard
import io.github.tatooinoyo.star.badge.ui.component.FilterChipStyled
import io.github.tatooinoyo.star.badge.ui.component.LabeledInputField
import io.github.tatooinoyo.star.badge.ui.component.OutlinedOrangeButton
import io.github.tatooinoyo.star.badge.ui.component.PanelNotch
import io.github.tatooinoyo.star.badge.ui.component.PrimaryOrangeButton
import io.github.tatooinoyo.star.badge.ui.component.SecondaryScreenHeader
import io.github.tatooinoyo.star.badge.ui.home.badge_sync.BadgeSyncViewModel
import io.github.tatooinoyo.star.badge.ui.home.component.BadgeFunctionArea
import io.github.tatooinoyo.star.badge.ui.home.component.BadgeReorderList
import io.github.tatooinoyo.star.badge.ui.home.component.ShareImportDialog
import io.github.tatooinoyo.star.badge.ui.home.component.TagFilterBar
import io.github.tatooinoyo.star.badge.ui.home.component.TagManageDialog
import io.github.tatooinoyo.star.badge.ui.icons.BadgeIcons
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrangeLight
import io.github.tatooinoyo.star.badge.ui.theme.PeachTheme
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import io.github.tatooinoyo.star.badge.ui.theme.TextSecondary
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareFormat
import io.github.tatooinoyo.star.badge.utils.update.UpdateCheckResult
import io.github.tatooinoyo.star.badge.utils.update.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
fun HomeScreen(
    nfcPayload: String? = null,
    onNfcDataConsumed: () -> Unit = {},
    homeViewModel: HomeViewModel? = null,
    badgeSyncViewModel: BadgeSyncViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToUnrecordedBadges: () -> Unit = {}
) {
    // 如果传入了 homeViewModel，则使用它；否则使用 viewModel() 创建
    val viewModelInstance = homeViewModel ?: viewModel()
    val uiState by viewModelInstance.uiState.collectAsState()
    val syncState by badgeSyncViewModel.syncState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val listState = rememberLazyListState()
    val updateChecker = remember { UpdateChecker(context) }
    var silentUpdate by remember { mutableStateOf<UpdateCheckResult.Available?>(null) }

    LaunchedEffect(Unit) {
        delay(2_500)
        val result = updateChecker.checkSilently()
        if (result is UpdateCheckResult.Available &&
            !updateChecker.isDismissed(result.info.versionName)
        ) {
            silentUpdate = result
        }
    }

    LaunchedEffect(nfcPayload) {
        if (!nfcPayload.isNullOrEmpty()) {
            viewModelInstance.onNfcPayloadReceived(nfcPayload)
            onNfcDataConsumed()
        }

        viewModelInstance.uiEvent.collectLatest { event ->
            when (event) {
                is BadgeUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                    // 自动录入成功后，清除输入框焦点
                    focusManager.clearFocus()
                }
                is BadgeUiEvent.LaunchShareFile -> {
                    clipboardManager.setText(AnnotatedString(event.code))
                    Toast.makeText(
                        context,
                        context.getString(R.string.share_code_copied),
                        Toast.LENGTH_SHORT,
                    ).show()
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = event.mimeType
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        clipData = ClipData.newUri(
                            context.contentResolver,
                            "badge_share",
                            event.uri,
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(
                            shareIntent,
                            context.getString(R.string.share_chooser_title),
                        )
                    )
                }
                is BadgeUiEvent.CopyTextToClipboard -> {
                    clipboardManager.setText(AnnotatedString(event.text))
                    Toast.makeText(context, event.toastMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    AnimatedContent(
        targetState = uiState.editingBadge,
        contentKey = { badge -> badge?.id ?: "badge-list" },
        transitionSpec = {
            val enteringDetail = targetState != null
            val transform = if (enteringDetail) {
                (slideInHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> width },
                ) + fadeIn(animationSpec = tween(180, delayMillis = 40))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        targetOffsetX = { width -> -width / 5 },
                    ) + fadeOut(animationSpec = tween(160)))
            } else {
                (slideInHorizontally(
                    animationSpec = tween(260, easing = FastOutSlowInEasing),
                    initialOffsetX = { width -> -width / 5 },
                ) + fadeIn(animationSpec = tween(200, delayMillis = 30))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetX = { width -> width },
                    ) + fadeOut(animationSpec = tween(180)))
            }
            transform.using(SizeTransform(clip = false))
        },
        label = "badgeDetailTransition",
    ) { editingBadge ->
    if (editingBadge == null) {
        BadgeListContent(
            uiState = uiState,
            listState = listState,
            onInputTitleChange = { viewModelInstance.updateAddInput(title = it) },
            onInputRemarkChange = { viewModelInstance.updateAddInput(remark = it) },
            onInputLinkChange = { viewModelInstance.updateAddInput(link = it) },
            onInputChannelChange = { viewModelInstance.updateAddInput(channel = it) },
            onFastModeChange = viewModelInstance::toggleFastMode,
            onAddClick = { viewModelInstance.addBadge() },
            onItemClick = { badge ->
                if (uiState.isShareSelecting) {
                    viewModelInstance.toggleShareSelection(badge.id)
                } else {
                    viewModelInstance.selectBadge(badge)
                }
            },
            onToggleFunctionArea = { viewModelInstance.toggleFunctionArea() },
            onSetFunctionAreaExpanded = { viewModelInstance.setFunctionAreaExpanded(it) },
            onExtractSkClick = { link -> viewModelInstance.extractSkFromLink(link) },
            onTagsChange = { viewModelInstance.updateAddInput(tags = it) },
            onSharedFileSelected = viewModelInstance::receiveShareImport,
            onTagSelected = { tag -> viewModelInstance.selectTag(tag) },
            onMove = { badgeId, insertBeforeId ->
                viewModelInstance.moveBadge(badgeId, insertBeforeId)
            },
            onSaveOrder = { viewModelInstance.saveOrder() },
            // === 传递同步参数 ===
            syncState = syncState,
            onStartSender = { badgeSyncViewModel.startSenderMode() },
            onStopSender = { badgeSyncViewModel.stopSenderMode() },
            onStartReceiver = { code -> badgeSyncViewModel.startReceiverMode(code) },
            onStopReceiver = { badgeSyncViewModel.stopReceiverMode() },
            onConfirmImport = { badgeSyncViewModel.confirmImport() },
            onCancelImport = { badgeSyncViewModel.cancelImport() },
            onImport = { ctx, uri, onResult ->
                viewModelInstance.importBadgesFromUri(ctx, uri, onResult)
            },
            onExport = { ctx, uri, onResult ->
                viewModelInstance.exportBadgesToUri(ctx, uri, onResult)
            },
            onShareSelectBadges = { viewModelInstance.enterShareSelection() },
            onShareExport = { viewModelInstance.prepareShareExport() },
            onShareFormatChange = { viewModelInstance.setShareFormat(it) },
            onCopyShareCode = { code ->
                clipboardManager.setText(AnnotatedString(code))
                Toast.makeText(
                    context,
                    context.getString(R.string.msg_copy_success),
                    Toast.LENGTH_SHORT,
                ).show()
            },
            onCancelShareSelection = { viewModelInstance.cancelShareSelection() },
            onSelectAllForShare = { viewModelInstance.selectAllVisibleForShare() },
            onFinishShareSelection = { viewModelInstance.finishShareSelection() },
            // 导航相关
            onSettingsClick = onNavigateToSettings,
            onAboutClick = onNavigateToAbout,
            onUnrecordedBadgesClick = onNavigateToUnrecordedBadges
        )
    } else {
        BadgeDetailContent(
            badge = editingBadge,
            title = uiState.detailTitle,
            remark = uiState.detailRemark,
            link = uiState.detailLink,
            channel = uiState.detailChannel,
            availableChannels = uiState.availableChannels,
            tags = uiState.detailTags,
            allTags = uiState.allTags,
            onTagsChange = { viewModelInstance.updateDetailInput(tags = it) },
            isWritingNfc = uiState.isWritingNfc,
            isNfcWriteInProgress = uiState.isNfcWriteInProgress,
            onTitleChange = { viewModelInstance.updateDetailInput(title = it) },
            onRemarkChange = { viewModelInstance.updateDetailInput(remark = it) },
            onLinkChange = { viewModelInstance.updateDetailInput(link = it) },
            onChannelChange = { viewModelInstance.updateDetailInput(channel = it) },
            onWriteNfcClick = { viewModelInstance.startWritingNfc() },
            onCancelWriteNfcClick = { viewModelInstance.cancelWritingNfc() },
            onSaveClick = { viewModelInstance.saveBadgeUpdate() },
            onDeleteClick = { viewModelInstance.deleteBadge() },
            onExitClick = { viewModelInstance.exitEditMode() },
            onExtractSkClick = { link -> viewModelInstance.extractSkFromLink(link) }
        )
    }
    }

    uiState.extractedSk?.let { sk ->
        AlertDialog(
            onDismissRequest = { viewModelInstance.dismissSkDialog() },
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
                TextButton(onClick = { viewModelInstance.dismissSkDialog() }) {
                    Text(stringResource(R.string.btn_confirm))
                }
            }
        )
    }

    silentUpdate?.let { available ->
        UpdateCheckDialog(
            result = available,
            loading = false,
            onDismiss = { silentUpdate = null },
            onDismissVersion = {
                updateChecker.dismissVersion(it)
                silentUpdate = null
            },
        )
    }

    uiState.pendingImportUri?.let { uri ->
        ShareImportDialog(
            uri = uri,
            onDismiss = { viewModelInstance.dismissShareImport() },
            onImportSharedBadges = { ctx, importUri, code, onResult ->
                viewModelInstance.importSharedBadges(ctx, importUri, code, onResult)
            },
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
    onInputChannelChange: (BadgeChannelTarget) -> Unit,
    onFastModeChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onItemClick: (Badge) -> Unit,
    onToggleFunctionArea: () -> Unit,
    onSetFunctionAreaExpanded: (Boolean) -> Unit,
    onExtractSkClick: (String) -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onSharedFileSelected: (Uri) -> Unit,
    onTagSelected: (String?) -> Unit = {},
    onMove: (String, String?) -> Boolean,
    onSaveOrder: () -> Unit,
    // 同步相关参数 ===
    syncState: SyncState,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onStartReceiver: (String) -> Unit,
    onStopReceiver: () -> Unit,
    onConfirmImport: () -> Unit = {},
    onCancelImport: () -> Unit = {},
    onImport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onExport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onShareSelectBadges: () -> Unit = {},
    onShareExport: () -> Unit = {},
    onShareFormatChange: (BadgeShareFormat) -> Unit = {},
    onCopyShareCode: (String) -> Unit = {},
    onCancelShareSelection: () -> Unit = {},
    onSelectAllForShare: () -> Unit = {},
    onFinishShareSelection: () -> Unit = {},
    // 导航相关
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onUnrecordedBadgesClick: () -> Unit,
) {
    val listExpanded = uiState.isShareSelecting || !uiState.isFunctionAreaExpanded
    val density = LocalDensity.current

    BackHandler(enabled = uiState.isShareSelecting || listExpanded) {
        if (uiState.isShareSelecting) {
            onCancelShareSelection()
        } else {
            onSetFunctionAreaExpanded(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                ),
            ),
    ) {
        if (uiState.isShareSelecting) {
            ShareSelectionTopBar(
                selectedCount = uiState.shareSelectedIds.size,
                onCancel = onCancelShareSelection,
                onSelectAll = onSelectAllForShare,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val fullHeightPx = constraints.maxHeight.toFloat()
            // 只用导航栏 inset，排除 IME，避免键盘弹出时折叠把手被顶起
            val bottomInsetPx = WindowInsets.navigationBars.getBottom(density).toFloat()
            val collapsedHeightPx =
                with(density) { LIST_HANDLE_HEIGHT.toPx() } + bottomInsetPx
            val collapsedHeightDp = with(density) { collapsedHeightPx.toDp() }
            val sheetHeightPx by animateFloatAsState(
                targetValue = if (listExpanded) fullHeightPx else collapsedHeightPx,
                animationSpec = tween(
                    durationMillis = LIST_SHEET_ANIM_MS,
                    easing = FastOutSlowInEasing,
                ),
                label = "badgeListSheetHeight",
            )
            val sheetHeightDp = with(density) { sheetHeightPx.toDp() }
            val showListContent =
                sheetHeightPx > collapsedHeightPx + with(density) { 24.dp.toPx() }

            // 功能区固定铺满底层，不随面板高度被挤压；imePadding 只作用于此，列表把手不受影响
            if (!uiState.isShareSelecting) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = collapsedHeightDp)
                        .imePadding()
                        .zIndex(0f),
                ) {
                    BadgeFunctionArea(
                        uiState = uiState,
                        syncState = syncState,
                        onInputTitleChange = onInputTitleChange,
                        onInputRemarkChange = onInputRemarkChange,
                        onInputLinkChange = onInputLinkChange,
                        onInputChannelChange = onInputChannelChange,
                        onFastModeChange = onFastModeChange,
                        onAddClick = onAddClick,
                        onExtractSkClick = onExtractSkClick,
                        onTagsChange = onTagsChange,
                        onSharedFileSelected = onSharedFileSelected,
                        onStartSender = onStartSender,
                        onStopSender = onStopSender,
                        onStartReceiver = onStartReceiver,
                        onStopReceiver = onStopReceiver,
                        onConfirmImport = onConfirmImport,
                        onCancelImport = onCancelImport,
                        onImport = onImport,
                        onExport = onExport,
                        allBadges = uiState.allBadgesUnfiltered,
                        onShareSelectBadges = onShareSelectBadges,
                        onShareExport = onShareExport,
                        onShareFormatChange = onShareFormatChange,
                        onCopyShareCode = onCopyShareCode,
                        onSettingsClick = onSettingsClick,
                        onAboutClick = onAboutClick,
                        onUnrecordedBadgesClick = onUnrecordedBadgesClick,
                    )
                }
            }

            // 列表面板叠在功能区之上，从底部滑入覆盖
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(sheetHeightDp)
                    .zIndex(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .border(
                        width = 1.dp,
                        color = BorderDefault,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clipToBounds(),
            ) {
                BadgeListDragHandle(
                    listExpanded = listExpanded,
                    onExpand = { onSetFunctionAreaExpanded(false) },
                    onCollapse = { onSetFunctionAreaExpanded(true) },
                )

                if (showListContent) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp),
                    ) {
                        TagFilterBar(
                            allTags = uiState.allTags,
                            selectedTag = uiState.selectedTag,
                            onTagSelected = onTagSelected,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        BadgeReorderList(
                            badges = uiState.badges,
                            onItemClick = onItemClick,
                            onMove = onMove,
                            onSaveOrder = onSaveOrder,
                            listState = listState,
                            isShareSelecting = uiState.isShareSelecting,
                            shareSelectedIds = uiState.shareSelectedIds,
                            modifier = Modifier
                                .weight(1f)
                                .swipeToChangeTag(
                                    allTags = uiState.allTags,
                                    selectedTag = uiState.selectedTag,
                                    onTagSelected = onTagSelected,
                                ),
                        )
                    }
                } else {
                    // 折叠：把手下方用面板色铺满到底
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface),
                    )
                }
            }
        }

        if (uiState.isShareSelecting) {
            ShareSelectionBottomBar(
                selectedCount = uiState.shareSelectedIds.size,
                onNext = onFinishShareSelection,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding(),
            )
        }
    }
}

/** 把手区域高度：上 16 + 条 4 + 下 16 */
private val LIST_HANDLE_HEIGHT = 36.dp
private const val LIST_SHEET_ANIM_MS = 1300
private const val LIST_HANDLE_DRAG_THRESHOLD_PX = 80f
private val TAG_FILTER_SWIPE_THRESHOLD = 56.dp

/**
 * 在列表內容上橫向滑動時切換相鄰標籤。橫向手勢辨識器會與 LazyColumn 的
 * 垂直滾動競爭方向，因此正常上下捲動不會觸發篩選切換。
 */
private fun Modifier.swipeToChangeTag(
    allTags: List<String>,
    selectedTag: String?,
    onTagSelected: (String?) -> Unit,
): Modifier = pointerInput(allTags, selectedTag) {
    if (allTags.isEmpty()) return@pointerInput

    val options: List<String?> = listOf(null) + allTags
    val selectedIndex = options.indexOf(selectedTag).takeIf { it >= 0 } ?: 0
    val thresholdPx = TAG_FILTER_SWIPE_THRESHOLD.toPx()
    var totalDrag = 0f

    detectHorizontalDragGestures(
        onDragStart = { totalDrag = 0f },
        onHorizontalDrag = { change, dragAmount ->
            change.consume()
            totalDrag += dragAmount
        },
        onDragCancel = { totalDrag = 0f },
        onDragEnd = {
            val targetIndex = when {
                totalDrag <= -thresholdPx -> selectedIndex + 1
                totalDrag >= thresholdPx -> selectedIndex - 1
                else -> selectedIndex
            }
            if (targetIndex != selectedIndex && targetIndex in options.indices) {
                onTagSelected(options[targetIndex])
            }
            totalDrag = 0f
        },
    )
}

@Composable
private fun BadgeListDragHandle(
    listExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(LIST_HANDLE_HEIGHT)
            .pointerInput(listExpanded) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                    },
                    onDragEnd = {
                        when {
                            !listExpanded && totalDrag <= -LIST_HANDLE_DRAG_THRESHOLD_PX -> onExpand()
                            listExpanded && totalDrag >= LIST_HANDLE_DRAG_THRESHOLD_PX -> onCollapse()
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        PanelNotch()
    }
}

@Composable
private fun ShareSelectionTopBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onSelectAll: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.cancel))
        }
        Text(
            text = stringResource(R.string.share_selected_count, selectedCount),
            style = MaterialTheme.typography.titleSmall,
        )
        TextButton(onClick = onSelectAll) {
            Text(stringResource(R.string.share_select_all_visible))
        }
    }
}

@Composable
private fun ShareSelectionBottomBar(
    selectedCount: Int,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Button(
            onClick = onNext,
            enabled = selectedCount > 0,
        ) {
            Text(stringResource(R.string.share_next_step))
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
    channel: BadgeChannelTarget?,
    availableChannels: List<BadgeChannelTarget>,
    tags: List<String>,
    allTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    isWritingNfc: Boolean,
    isNfcWriteInProgress: Boolean,
    onTitleChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onLinkChange: (String) -> Unit,
    onChannelChange: (BadgeChannelTarget) -> Unit,
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
            .safeDrawingPadding(),
    ) {
        SecondaryScreenHeader(
            title = stringResource(R.string.edit_badge_details),
            onBack = onExitClick,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            BadgeInputForm(
                title = title,
                onTitleChange = onTitleChange,
                remark = remark,
                onRemarkChange = onRemarkChange,
                link = link,
                onLinkChange = onLinkChange,
                channel = channel,
                availableChannels = availableChannels,
                onChannelChange = onChannelChange,
                allTags = allTags,
                selectedTags = tags,
                onTagsChange = onTagsChange,
                onExtractSkClick = onExtractSkClick,
            )
        }

        BadgeContentCard(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                PrimaryOrangeButton(
                    text = stringResource(R.string.write_link_to_nfc_card),
                    onClick = onWriteNfcClick,
                    icon = BadgeIcons.IdCard,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedOrangeButton(
                        text = stringResource(R.string.exit),
                        onClick = onExitClick,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.White)
                            .border(1.dp, BrandOrange.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                            .clickable { showDeleteConfirm = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.delete),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandOrange,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(Color.White)
                            .border(1.dp, BrandOrange, MaterialTheme.shapes.small)
                            .clickable { showUpdateConfirm = true }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.save_update),
                            style = MaterialTheme.typography.labelLarge,
                            color = BrandOrange,
                        )
                    }
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
            title = {
                Text(
                    stringResource(
                        if (isNfcWriteInProgress) R.string.writing_nfc
                        else R.string.prepare_write_nfc,
                    )
                )
            },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isNfcWriteInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                    Text(
                        if (isNfcWriteInProgress) {
                            stringResource(R.string.writing_nfc_message)
                        } else {
                            stringResource(R.string.prepare_write_nfc_message, link)
                        }
                    )
                }
            },
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
    channel: BadgeChannelTarget?, onChannelChange: (BadgeChannelTarget) -> Unit,
    availableChannels: List<BadgeChannelTarget>,
    onExtractSkClick: (String) -> Unit, // 新增回调
    allTags: List<String> = emptyList(),
    selectedTags: List<String> = emptyList(),
    onTagsChange: (List<String>) -> Unit,
    isFastMode: Boolean = false
) {
    var channelMenuExpanded by remember { mutableStateOf(false) }
    var otherAppsExpanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var lastLinkContent by remember { mutableStateOf("") }
    var showTagDialog by remember { mutableStateOf(false) }
    val directChannels = availableChannels.filter { it.acceptsBadgeLinks }
    val otherApps = availableChannels.filterNot { it.acceptsBadgeLinks }
    if (otherAppsExpanded) {
        AlertDialog(
            onDismissRequest = { otherAppsExpanded = false },
            title = { Text(stringResource(R.string.channel_choose_other_app)) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(otherApps, key = { it.packageName }) { option ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onChannelChange(option)
                                    otherAppsExpanded = false
                                }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(option.getLabel(LocalContext.current))
                            Text(
                                option.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { otherAppsExpanded = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
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

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LabeledInputField(
            label = stringResource(R.string.title),
            value = title,
            placeholder = stringResource(R.string.title_placeholder),
            onValueChange = onTitleChange,
        )

        LabeledInputField(
            label = stringResource(R.string.remark),
            value = remark,
            placeholder = stringResource(R.string.remark_placeholder),
            onValueChange = onRemarkChange,
        )

        LabeledInputField(
            label = stringResource(R.string.link),
            value = link,
            placeholder = stringResource(R.string.link_placeholder),
            onValueChange = onLinkChange,
            modifier = Modifier.onFocusChanged { focusState ->
                if (focusState.isFocused && isFastMode) {
                    val clipboardContent = clipboardManager.getText()?.text
                    if (!clipboardContent.isNullOrBlank() &&
                        (clipboardContent.startsWith("http", ignoreCase = true) ||
                            clipboardContent.startsWith("sky", ignoreCase = true))
                    ) {
                        if (lastLinkContent != clipboardContent) {
                            lastLinkContent = clipboardContent
                            onLinkChange(clipboardContent)
                        }
                    }
                }
            },
            trailing = {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(BrandOrange)
                        .clickable { onExtractSkClick(link) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "SK",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    )
                }
            },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {

            Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                val arrowRotation by animateFloatAsState(
                    targetValue = if (channelMenuExpanded) 180f else 0f,
                    label = "channelArrowRotation",
                )
                Row(
                    modifier = Modifier
                        .width(88.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = if (channelMenuExpanded) BrandOrange else BorderDefault,
                            shape = MaterialTheme.shapes.small,
                        )
                        .clickable { channelMenuExpanded = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.channel_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = channel?.getLabel(LocalContext.current)
                                    ?: stringResource(R.string.channel_none_installed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .size(22.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(BrandOrangeLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = BrandOrange,
                            modifier = Modifier.size(16.dp).rotate(arrowRotation),
                        )
                    }
                }

                DropdownMenu(
                    expanded = channelMenuExpanded,
                    onDismissRequest = { channelMenuExpanded = false },
                    modifier = Modifier.width(280.dp),
                    shape = MaterialTheme.shapes.medium,
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 8.dp,
                ) {
                    directChannels.forEach { option ->
                        val selected = channel?.packageName == option.packageName
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.getLabel(LocalContext.current),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (selected) BrandOrange else TextPrimary,
                                            fontWeight = if (selected) {
                                                androidx.compose.ui.text.font.FontWeight.SemiBold
                                            } else {
                                                androidx.compose.ui.text.font.FontWeight.Normal
                                            },
                                        )
                                        if (option.isInstalled) {
                                            Surface(
                                                modifier = Modifier.padding(start = 8.dp),
                                                color = BrandOrangeLight,
                                                shape = MaterialTheme.shapes.extraSmall,
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.channel_installed),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = BrandOrange,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                    if (option.channel == io.github.tatooinoyo.star.badge.data.BadgeChannel.CUSTOM) {
                                        Text(
                                            option.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            },
                            trailingIcon = if (selected) {
                                {
                                    Icon(
                                        painter = painterResource(BadgeIcons.Check),
                                        contentDescription = null,
                                        tint = BrandOrange,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else null,
                            onClick = {
                                onChannelChange(option)
                                channelMenuExpanded = false
                            },
                            modifier = Modifier.background(
                                if (selected) BrandOrangeLight.copy(alpha = 0.45f) else Color.Transparent
                            ),
                        )
                    }
                    if (otherApps.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = BorderDefault,
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.channel_choose_other_app),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandOrange,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                )
                            },
                            onClick = {
                                channelMenuExpanded = false
                                otherAppsExpanded = true
                            },
                        )
                    }
                    if (directChannels.isEmpty() && otherApps.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.channel_none_installed)) },
                            onClick = {},
                            enabled = false,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .then(
                        if (selectedTags.isEmpty()) {
                            Modifier.clickable { showTagDialog = true }
                        } else {
                            Modifier
                        }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. 管理按钮
                IconButton(onClick = { showTagDialog = true }) {
                    Icon(
                        painter = painterResource(BadgeIcons.Label),
                        contentDescription = stringResource(R.string.manage_tags),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // 2. 显示当前已选标签；未选时提示并可整行点击打开管理弹窗
                if (selectedTags.isEmpty()) {
                    Text(
                        text = stringResource(R.string.add_tags),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        items(selectedTags) { tag ->
                            FilterChipStyled(
                                label = tag,
                                selected = true,
                                showCheck = false,
                                onClick = { showTagDialog = true },
                            )
                        }
                    }
                }
            }
        }
    }
}
