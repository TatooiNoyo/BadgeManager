package io.github.tatooinoyo.star.badge.ui.home

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannelTarget
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.data.InstalledChannelProvider
import io.github.tatooinoyo.star.badge.utils.LanguageManager
import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator
import io.github.tatooinoyo.star.badge.utils.readBoundedBytes
import io.github.tatooinoyo.star.badge.utils.preset.PresetResolver
import io.github.tatooinoyo.star.badge.utils.SkExtractException
import io.github.tatooinoyo.star.badge.utils.SkExtractor
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareCrypto
import io.github.tatooinoyo.star.badge.utils.export.BadgeBackupExporter
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareError
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareExporter
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareFormat
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareText
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

// 定义 UI 状态
data class BadgeUiState(
    val badges: List<Badge> = emptyList(),
    val allBadgesUnfiltered: List<Badge> = emptyList(),
    val editingBadge: Badge? = null, // 如果不为空，则显示详情/编辑页
    // 添加模式下的输入状态
    val addTitle: String = "",
    val addRemark: String = "",
    val addLink: String = "",
    val addChannel: BadgeChannelTarget? = null,
    val availableChannels: List<BadgeChannelTarget> = emptyList(),
    val isFastMode: Boolean = false,
    // 详情模式下的输入状态 (如果是分离的页面，也可以拆分 ViewModel，这里为了简单先放一起)
    val detailTitle: String = "",
    val detailRemark: String = "",
    val detailLink: String = "",
    val detailChannel: BadgeChannelTarget? = null,
    val detailTags: List<String> = emptyList(),
    val isWritingNfc: Boolean = false, // 是否正在等待写入 NFC
    val isNfcWriteInProgress: Boolean = false,
    val extractedSk: String? = null, // 提取出的 SK 编码
    val isFunctionAreaExpanded: Boolean = true, // 记录功能区是否展开
    val selectedTag: String? = null,
    val addTags: List<String> = emptyList(), // 当前新增徽章的标签
    val allTags: List<String> = emptyList(),
    // 分享相关
    val isShareSelecting: Boolean = false,
    val shareSelectedIds: Set<String> = emptySet(),
    val pendingShareCode: String? = null,
    val shareFormat: BadgeShareFormat = BadgeShareFormat.TEXT,
    val pendingImportUri: android.net.Uri? = null,
    val functionTabIndex: Int = 0,
)

// 定义一次性 UI 事件
sealed class BadgeUiEvent {
    data class ShowToast(val message: String) : BadgeUiEvent()
    data class LaunchShareFile(
        val uri: android.net.Uri,
        val code: String,
        val mimeType: String,
    ) : BadgeUiEvent()

    data class CopyTextToClipboard(
        val text: String,
        val toastMessage: String,
    ) : BadgeUiEvent()
}

private data class BadgeListFilterResult(
    val filteredBadges: List<Badge>,
    val allBadges: List<Badge>,
    val allTags: List<String>,
    val selectedTag: String?,
)

private data class NfcWriteOutcome(
    val success: Boolean,
    val messageRes: Int,
    val detail: String? = null,
)

class HomeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {

    private val preferences = application.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        BadgeUiState(
            pendingImportUri = savedStateHandle.get<String>(PENDING_IMPORT_URI)?.let(android.net.Uri::parse),
        )
    )
    val uiState: StateFlow<BadgeUiState> = _uiState.asStateFlow()

    // 创建事件流
    private val _uiEvent = MutableSharedFlow<BadgeUiEvent>()
    val uiEvent: SharedFlow<BadgeUiEvent> = _uiEvent.asSharedFlow()

    private val _selectedTag = MutableStateFlow<String?>(null) // null 代表显示全部
    private var nfcWriteGeneration = 0L
    private var nfcWriteJob: Job? = null
    private var receiveShareJob: Job? = null
    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshInstalledChannels()
        }
    }

    init {
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= 33) {
            application.registerReceiver(packageChangeReceiver, packageFilter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            application.registerReceiver(packageChangeReceiver, packageFilter)
        }
        refreshInstalledChannels()
        // 收集 Repository 的 Flow 和 选中的标签 Flow，合并计算后更新 UI 状态
        viewModelScope.launch {
            combine(
                BadgeRepository.badges,
                _selectedTag
            ) { allBadges, selectedTag ->
                // 1. 计算所有标签 (基于完整列表)
                val allTags = allBadges.flatMap { it.tags }.distinct().sorted()

                // 2. 根据选中的标签过滤显示列表
                val filteredBadges = if (selectedTag == null) {
                    allBadges
                } else {
                    allBadges.filter { it.tags.contains(selectedTag) }
                }

                // 返回过滤结果供 collect 使用
                BadgeListFilterResult(filteredBadges, allBadges, allTags, selectedTag)
            }.flowOn(kotlinx.coroutines.Dispatchers.Default).collect { result ->
                _uiState.update {
                    it.copy(
                        badges = result.filteredBadges,
                        allBadgesUnfiltered = result.allBadges,
                        allTags = result.allTags,
                        selectedTag = result.selectedTag,
                    )
                }
            }
        }
        // 进程被回收时若文件尚未复制完，恢复待处理请求；普通重建继续使用原 ViewModel。
        savedStateHandle.get<String>(INCOMING_IMPORT_URI)?.let {
            receiveShareImport(android.net.Uri.parse(it))
        }
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageChangeReceiver) }
        super.onCleared()
    }

    fun refreshInstalledChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            val channels = InstalledChannelProvider.discover(getApplication())
            _uiState.update { state ->
                val savedPackageName = preferences.getString(KEY_ADD_CHANNEL_PACKAGE, null)
                val restoredChannel = savedPackageName?.let { packageName ->
                    channels.firstOrNull { it.packageName == packageName }
                        ?: InstalledChannelProvider.discoverOtherApps(
                            getApplication(),
                            channels.mapTo(HashSet()) { it.packageName },
                        ).firstOrNull { it.packageName == packageName }
                }
                state.copy(
                    availableChannels = channels,
                    addChannel = restoredChannel ?: state.addChannel ?: channels.firstOrNull {
                        it.acceptsBadgeLinks && it.isInstalled
                    }.also { channel ->
                        // Persist the initial fallback when there is no usable saved target.
                        if (restoredChannel == null && state.addChannel == null && channel != null) {
                            preferences.edit().putString(KEY_ADD_CHANNEL_PACKAGE, channel.packageName).apply()
                        }
                    },
                )
            }
        }
    }

    fun toggleFunctionArea() {
        _uiState.update { it.copy(isFunctionAreaExpanded = !it.isFunctionAreaExpanded) }
    }

    fun setFunctionAreaExpanded(expanded: Boolean) {
        if (_uiState.value.isFunctionAreaExpanded == expanded) return
        _uiState.update { it.copy(isFunctionAreaExpanded = expanded) }
    }

    fun setFunctionTabIndex(index: Int) {
        _uiState.update { it.copy(functionTabIndex = index) }
    }

    // === 分享选择模式 ===

    fun enterShareSelection() {
        _uiState.update {
            it.copy(
                isShareSelecting = true,
                isFunctionAreaExpanded = false,
            )
        }
    }

    fun cancelShareSelection() {
        _uiState.update {
            it.copy(
                isShareSelecting = false,
                shareSelectedIds = emptySet(),
            )
        }
    }

    fun toggleShareSelection(badgeId: String) {
        _uiState.update { state ->
            val updated = state.shareSelectedIds.toMutableSet()
            if (updated.contains(badgeId)) {
                updated.remove(badgeId)
            } else {
                updated.add(badgeId)
            }
            state.copy(shareSelectedIds = updated)
        }
    }

    fun selectAllVisibleForShare() {
        _uiState.update { state ->
            state.copy(shareSelectedIds = state.badges.map { it.id }.toSet())
        }
    }

    fun finishShareSelection() {
        if (_uiState.value.shareSelectedIds.isEmpty()) return
        _uiState.update {
            it.copy(
                isShareSelecting = false,
                isFunctionAreaExpanded = true,
                functionTabIndex = 2,
            )
        }
    }

    fun clearPendingShareCode() {
        _uiState.update { it.copy(pendingShareCode = null) }
    }

    fun setShareFormat(format: BadgeShareFormat) {
        _uiState.update { it.copy(shareFormat = format) }
    }

    fun dismissShareImport() {
        savedStateHandle.remove<String>(PENDING_IMPORT_URI)
        _uiState.update {
            it.copy(
                pendingImportUri = null,
            )
        }
    }

    fun receiveShareImport(uri: android.net.Uri) {
        receiveShareJob?.cancel()
        savedStateHandle[INCOMING_IMPORT_URI] = uri.toString()
        receiveShareJob = viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val stableUri = withContext(Dispatchers.IO) {
                    BadgeShareExporter.copyIncomingShareToCache(context, uri)
                }
                savedStateHandle[PENDING_IMPORT_URI] = stableUri.toString()
                savedStateHandle.remove<String>(INCOMING_IMPORT_URI)
                _uiState.update {
                    it.copy(
                        pendingImportUri = stableUri,
                        isFunctionAreaExpanded = true,
                        functionTabIndex = 0,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: BadgeShareError.InvalidFile) {
                savedStateHandle.remove<String>(INCOMING_IMPORT_URI)
                Log.w("HomeViewModel", "Incoming share is not a badgeenc file", e)
                _uiEvent.emit(
                    BadgeUiEvent.ShowToast(
                        getApplication<Application>().getString(R.string.share_import_invalid_file)
                    )
                )
            } catch (e: Exception) {
                savedStateHandle.remove<String>(INCOMING_IMPORT_URI)
                Log.e("HomeViewModel", "Failed to receive shared file", e)
                _uiEvent.emit(
                    BadgeUiEvent.ShowToast(
                        getApplication<Application>().getString(R.string.share_import_failed)
                    )
                )
            }
        }
    }

    fun prepareShareExport() {
        val state = _uiState.value
        val selectedIds = state.shareSelectedIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val allBadges = BadgeRepository.badges.value
                val toShare = allBadges.filter { it.id in selectedIds }
                if (toShare.isEmpty()) return@launch

                when (state.shareFormat) {
                    BadgeShareFormat.TEXT -> {
                        val entries = BadgeShareText.toEntries(toShare)
                        if (entries.isEmpty()) {
                            _uiEvent.emit(
                                BadgeUiEvent.ShowToast(
                                    getApplication<Application>().getString(
                                        R.string.share_text_no_entries
                                    )
                                )
                            )
                            return@launch
                        }
                        val text = BadgeShareText.formatPlain(toShare)
                        val toast = getApplication<Application>().getString(
                            R.string.share_copied_to_clipboard,
                            text.length,
                        )
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _uiState.update { it.copy(pendingShareCode = null) }
                            _uiEvent.emit(BadgeUiEvent.CopyTextToClipboard(text, toast))
                        }
                    }
                    BadgeShareFormat.ENCRYPTED_FILE -> {
                        val code = BadgeShareCrypto.generateShareCode()
                        val context = getApplication<Application>()
                        val export = BadgeShareExporter.writeEncryptedFile(context, toShare, code)
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            _uiState.update { it.copy(pendingShareCode = code) }
                            _uiEvent.emit(
                                BadgeUiEvent.LaunchShareFile(
                                    export.uri,
                                    export.code,
                                    export.mimeType,
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Share export failed", e)
                _uiEvent.emit(
                    BadgeUiEvent.ShowToast(
                        getApplication<Application>().getString(R.string.share_export_failed)
                    )
                )
            }
        }
    }

    fun importSharedBadges(
        context: android.content.Context,
        uri: android.net.Uri,
        code: String,
        onResult: (Int) -> Unit,
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBoundedBytes(BadgeShareCrypto.MAX_FILE_BYTES) }
                    ?: run {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onResult(-1)
                        }
                        return@launch
                    }

                val result = BadgeShareCrypto.decrypt(bytes, code)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    val msgRes = when (error) {
                        is BadgeShareError.WrongPassword -> R.string.share_import_wrong_password
                        is BadgeShareError.InvalidFile,
                        is BadgeShareError.UnsupportedVersion -> R.string.share_import_invalid_file
                        is BadgeShareError.EmptyPayload -> R.string.share_import_empty
                        else -> R.string.share_import_failed
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _uiEvent.emit(
                            BadgeUiEvent.ShowToast(
                                getApplication<Application>().getString(msgRes)
                            )
                        )
                        onResult(-1)
                    }
                    return@launch
                }
                val envelope = result.getOrThrow()

                BadgeRepository.importBadgeCopies(envelope.badges)
                val imported = envelope.badges.size

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(imported)
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Share import failed", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiEvent.emit(
                        BadgeUiEvent.ShowToast(
                            getApplication<Application>().getString(R.string.share_import_failed)
                        )
                    )
                    onResult(-1)
                }
            }
        }
    }

    // === 列表/添加页面的操作 ===

    fun selectTag(tag: String?) {
        _selectedTag.value = tag
    }


    private fun normalizeLink(link: String): String {
        return if (link.isNotBlank() && !link.startsWith("http://") && !link.startsWith("https://")) {
            "https://$link"
        } else {
            link
        }
    }

    fun updateAddInput(
        title: String = _uiState.value.addTitle,
        remark: String = _uiState.value.addRemark,
        link: String = _uiState.value.addLink,
        channel: BadgeChannelTarget? = _uiState.value.addChannel,
        tags: List<String> = _uiState.value.addTags
    ) {
        val isLinkChanged = link != _uiState.value.addLink
        val previousChannel = _uiState.value.addChannel
        var finalTitle = title
        var finalRemark = remark

        // 只有在链接改变时，才尝试提取 SK 并覆盖标题/备注
        if (isLinkChanged && link.isNotBlank()) {
            tryFillPresetFromLink(link)?.let { (presetTitle, presetRemark) ->
                finalTitle = presetTitle
                finalRemark = presetRemark
            }
        }

        _uiState.value = _uiState.value.copy(
            addTitle = finalTitle,
            addRemark = finalRemark,
            addLink = link,
            addChannel = channel,
            addTags = tags // 确保更新状态
        )
        if (channel != previousChannel) {
            // Selection is persisted separately from the in-progress form fields.
            channel?.let {
                preferences.edit().putString(KEY_ADD_CHANNEL_PACKAGE, it.packageName).apply()
            }
        }
        if (_uiState.value.isFastMode && link.isNotBlank()) {
            addBadge()

            viewModelScope.launch {
                _uiEvent.emit(
                    BadgeUiEvent.ShowToast(
                        localizedContext().getString(R.string.toast_auto_added, finalTitle)
                    )
                )
            }
        }
    }

    fun toggleFastMode(enabled: Boolean) {
        _uiState.update { it.copy(isFastMode = enabled) }
    }

    fun addBadge() {
        val state = _uiState.value
        if (state.addTitle.isNotBlank() && state.addChannel != null) {
            // 兼容没有协议的链接
            val finalLink = normalizeLink(state.addLink)
            val title = state.addTitle
            val remark = state.addRemark
            val channel = state.addChannel
            val tags = state.addTags

            // 乐观重置输入，写库由 viewModelScope 驱动
            _uiState.value = state.copy(
                addTitle = "",
                addRemark = "",
                addLink = "",
                addTags = emptyList()
            )
            viewModelScope.launch {
                BadgeRepository.addBadge(title, remark, finalLink, channel, tags)
            }
        }
    }

    // === 详情页面的操作 ===

    fun selectBadge(badge: Badge) {
        // 进入编辑模式，初始化详情数据
        _uiState.value = _uiState.value.copy(
            editingBadge = badge,
            detailTitle = badge.title,
            detailRemark = badge.remark,
            detailLink = badge.link,
            detailChannel = badge.channelTarget(localizedContext()),
            detailTags = badge.tags
        )
    }

    fun updateDetailInput(
        title: String = _uiState.value.detailTitle,
        remark: String = _uiState.value.detailRemark,
        link: String = _uiState.value.detailLink,
        channel: BadgeChannelTarget? = _uiState.value.detailChannel,
        tags: List<String> = _uiState.value.detailTags
    ) {
        val isLinkChanged = link != _uiState.value.detailLink
        var finalTitle = title
        var finalRemark = remark

        // 只有在链接改变时，才尝试提取 SK 并覆盖标题/备注
        if (isLinkChanged && link.isNotBlank()) {
            tryFillPresetFromLink(link)?.let { (presetTitle, presetRemark) ->
                finalTitle = presetTitle
                finalRemark = presetRemark
            }
        }

        _uiState.value = _uiState.value.copy(
            detailTitle = finalTitle,
            detailRemark = finalRemark,
            detailLink = link,
            detailChannel = channel,
            detailTags = tags
        )
    }

    fun saveBadgeUpdate() {
        val state = _uiState.value
        state.editingBadge?.let { originalBadge ->
            val selectedChannel = state.detailChannel ?: return@let
            val finalLink = normalizeLink(state.detailLink)
            val title = state.detailTitle
            val remark = state.detailRemark
            val tags = state.detailTags
            val orderIndex = originalBadge.orderIndex
            val id = originalBadge.id
            exitEditMode()
            viewModelScope.launch {
                BadgeRepository.updateBadge(
                    id,
                    title,
                    remark,
                    finalLink,
                    selectedChannel,
                    orderIndex,
                    tags
                )
            }
        }
    }

    fun deleteBadge() {
        val state = _uiState.value
        state.editingBadge?.let {
            val id = it.id
            exitEditMode()
            viewModelScope.launch {
                BadgeRepository.removeBadge(id)
            }
        }
    }

    fun exitEditMode() {
        nfcWriteGeneration++
        _uiState.value =
            _uiState.value.copy(
                editingBadge = null,
                isWritingNfc = false,
                isNfcWriteInProgress = false,
                extractedSk = null,
            )
    }

    // === 拖拽排序 ===
    fun moveBadge(badgeId: String, insertBeforeId: String?): Boolean {
        val reordered = reorderBadgesBefore(
            badges = _uiState.value.badges,
            badgeId = badgeId,
            insertBeforeId = insertBeforeId,
        ) ?: return false

        // 以稳定 ID 解析当前位置，避免数据库回写或标签切换后旧下标移动错误徽章。
        _uiState.value = _uiState.value.copy(badges = reordered)
        return true
    }

    /**
     * 保存排序：只重分配当前列表中徽章已有的 orderIndex，
     * 避免标签过滤下把子集重编号成 0..n-1 从而与未显示项冲突。
     *
     * 例：全局 A=0,B=1,C=2,D=3,E=4，过滤得 A,C,E；拖成 E,A,C
     * → 将原序号池 [0,2,4] 按新顺序赋给 E,A,C，得到 E=0,A=2,C=4（B/D 不变）。
     */
    fun saveOrder() {
        val reordered = _uiState.value.badges
        if (reordered.isEmpty()) return

        val orderIndices = reordered.map { it.orderIndex }.sorted()
        val updatedList = reordered.mapIndexed { index, badge ->
            badge.copy(orderIndex = orderIndices[index])
        }
        _uiState.update { state ->
            if (state.badges.map { it.id } == reordered.map { it.id }) {
                state.copy(badges = updatedList)
            } else {
                state
            }
        }
        viewModelScope.launch {
            BadgeRepository.updateBadgeOrder(updatedList)
        }
    }

    // === NFC 处理逻辑 ===
    fun onNfcPayloadReceived(payload: String?) {
        if (payload.isNullOrEmpty()) return

        val currentState = _uiState.value
        if (currentState.editingBadge != null) {
            // 如果在详情页，更新详情页的链接
            updateDetailInput(link = payload)
        } else {
            // 如果在列表页，更新添加框的链接
            updateAddInput(link = payload)
        }
    }

    // === NFC 写入逻辑 ===
    fun startWritingNfc() {
        nfcWriteGeneration++
        _uiState.value = _uiState.value.copy(
            isWritingNfc = true,
            isNfcWriteInProgress = false,
        )
    }

    fun cancelWritingNfc() {
        // 已经进入系统 NFC I/O 的任务不强制中断，以免标签写到一半；忽略其迟到结果。
        nfcWriteGeneration++
        _uiState.value = _uiState.value.copy(
            isWritingNfc = false,
            isNfcWriteInProgress = false,
        )
    }

    /**
     * 接收 NFC 标签并把阻塞的 Ndef I/O 交给后台线程。
     * 返回 true 表示该标签属于当前写入流程，调用方不应继续按读取事件处理。
     */
    fun writeNfcTag(tag: Tag): Boolean {
        val state = _uiState.value
        if (!state.isWritingNfc) return false
        if (state.isNfcWriteInProgress || nfcWriteJob?.isActive == true) return true

        val linkToWrite = state.detailLink
        if (linkToWrite.isBlank()) {
            viewModelScope.launch {
                _uiEvent.emit(
                    BadgeUiEvent.ShowToast(localizedContext().getString(R.string.nfc_write_link_empty))
                )
            }
            return true
        }

        val operation = ++nfcWriteGeneration
        _uiState.update { it.copy(isNfcWriteInProgress = true) }
        nfcWriteJob = viewModelScope.launch(Dispatchers.IO) {
            val outcome = writeNfcTagOnIo(tag, linkToWrite)
            withContext(Dispatchers.Main) {
                if (operation != nfcWriteGeneration) return@withContext

                _uiState.update {
                    it.copy(
                        isWritingNfc = !outcome.success,
                        isNfcWriteInProgress = false,
                    )
                }
                val context = localizedContext()
                val message = if (outcome.detail == null) {
                    context.getString(outcome.messageRes)
                } else {
                    context.getString(outcome.messageRes, outcome.detail)
                }
                _uiEvent.emit(BadgeUiEvent.ShowToast(message))
            }
        }
        return true
    }

    private fun writeNfcTagOnIo(tag: Tag, link: String): NfcWriteOutcome {
        val ndef = Ndef.get(tag)
            ?: return NfcWriteOutcome(false, R.string.nfc_write_not_ndef)
        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                NfcWriteOutcome(false, R.string.nfc_write_readonly)
            } else {
                val message = NdefMessage(arrayOf(NdefRecord.createUri(link)))
                if (ndef.maxSize < message.toByteArray().size) {
                    NfcWriteOutcome(false, R.string.nfc_write_insufficient_capacity)
                } else {
                    ndef.writeNdefMessage(message)
                    NfcWriteOutcome(true, R.string.nfc_write_success)
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "NFC write failed", e)
            NfcWriteOutcome(
                success = false,
                messageRes = R.string.nfc_write_failed,
                detail = e.localizedMessage ?: e.javaClass.simpleName,
            )
        } finally {
            try {
                ndef.close()
            } catch (e: IOException) {
                Log.w("HomeViewModel", "Failed to close NFC tag", e)
            }
        }
    }

    // === SK 提取逻辑 ===

    /** Application Context 不带用户所选语言；预设标题等字符串需用 wrapContext。 */
    private fun localizedContext() =
        LanguageManager.getInstance(getApplication()).wrapContext(getApplication())

    /** 解析成功且命中预设时返回标题/备注；解析失败或未收录时返回 null（不向用户报错）。 */
    private fun tryFillPresetFromLink(link: String): Pair<String, String>? {
        return try {
            val sk = SkExtractor.getSkFromLink(link)
            if (!sk.startsWith("SKY-")) return null
            val context = localizedContext()
            val presetTitle = PresetResolver.getTitle(context, sk)
            if (presetTitle.isEmpty()) return null
            presetTitle to PresetResolver.getRemark(context, sk)
        } catch (_: SkExtractException) {
            null
        }
    }

    private fun skExtractErrorMessage(e: SkExtractException): String {
        val context = localizedContext()
        return when (e.reason) {
            SkExtractException.Reason.MISSING_S_PARAM ->
                context.getString(R.string.sk_extract_missing_s)
            SkExtractException.Reason.MISSING_SK_PARAM ->
                context.getString(R.string.sk_extract_missing_sk)
            SkExtractException.Reason.DECODE_FAILED ->
                context.getString(
                    R.string.sk_extract_failed,
                    e.cause?.message ?: context.getString(R.string.sk_extract_failed_unknown)
                )
        }
    }

    fun extractSkFromLink(link: String) {
        viewModelScope.launch {
            try {
                val sk = SkExtractor.getSkFromLink(link)
                _uiState.value = _uiState.value.copy(extractedSk = sk)
            } catch (e: SkExtractException) {
                _uiEvent.emit(BadgeUiEvent.ShowToast(skExtractErrorMessage(e)))
            }
        }
    }

    fun dismissSkDialog() {
        _uiState.value = _uiState.value.copy(extractedSk = null)
    }

    // === 备份与还原 ===

    /**
     * 导出数据到指定的文件 URI
     */
    fun exportBadgesToUri(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val success = try {
                // 1. 获取数据快照
                val badges = BadgeRepository.getAllBadgesSnapshot()
                // 输出流存在，且写入、刷新、关闭均完成后才算成功。
                BadgeBackupExporter.write(badges) {
                    context.contentResolver.openOutputStream(uri)
                }
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Backup export failed", e)
                false
            }
            withContext(Dispatchers.Main) {
                onResult(success)
            }
        }
    }

    /**
     * 从指定的文件 URI 导入数据
     */
    fun importBadgesFromUri(
        context: android.content.Context,
        uri: android.net.Uri,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use {
                    String(it.readBoundedBytes(BadgeImportValidator.MAX_JSON_BYTES), Charsets.UTF_8)
                } ?: throw IOException("Cannot read backup")
                val badges = BadgeImportValidator.parse(jsonString)

                BadgeRepository.restoreBadges(badges)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                Log.e("BadgeManagerViewModel", "Error importing badges", e)
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "badge_manager_preferences"
        private const val KEY_ADD_CHANNEL_PACKAGE = "add_channel_package"
        private const val PENDING_IMPORT_URI = "pending_import_uri"
        private const val INCOMING_IMPORT_URI = "incoming_import_uri"
    }
}
