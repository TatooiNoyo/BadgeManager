package io.tatooinoyo.star.badge.ui.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.tatooinoyo.star.badge.data.Badge
import io.tatooinoyo.star.badge.data.BadgeChannel
import io.tatooinoyo.star.badge.data.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// 定义 UI 状态
data class BadgeUiState(
    val badges: List<Badge> = emptyList(),
    val editingBadge: Badge? = null, // 如果不为空，则显示详情/编辑页
    // 添加模式下的输入状态
    val addTitle: String = "",
    val addRemark: String = "",
    val addLink: String = "",
    val addChannel: BadgeChannel = BadgeChannel.HUAWEI,
    // 详情模式下的输入状态 (如果是分离的页面，也可以拆分 ViewModel，这里为了简单先放一起)
    val detailTitle: String = "",
    val detailRemark: String = "",
    val detailLink: String = "",
    val detailChannel: BadgeChannel = BadgeChannel.HUAWEI
)

class BadgeManagerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BadgeUiState())
    val uiState: StateFlow<BadgeUiState> = _uiState.asStateFlow()

    init {
        // 收集 Repository 的 Flow 并更新 UI 状态
        viewModelScope.launch {
            BadgeRepository.badges.collect { badgeList ->
                _uiState.value = _uiState.value.copy(badges = badgeList)
            }
        }
    }

    // === 列表/添加页面的操作 ===

    fun updateAddInput(
        title: String = _uiState.value.addTitle,
        remark: String = _uiState.value.addRemark,
        link: String = _uiState.value.addLink,
        channel: BadgeChannel = _uiState.value.addChannel
    ) {
        _uiState.value = _uiState.value.copy(
            addTitle = title,
            addRemark = remark,
            addLink = link,
            addChannel = channel
        )
    }

    fun addBadge() {
        val state = _uiState.value
        if (state.addTitle.isNotBlank()) {
            BadgeRepository.addBadge(state.addTitle, state.addRemark, state.addLink, state.addChannel)
            // 重置输入
            _uiState.value = state.copy(
                addTitle = "",
                addRemark = "",
                addLink = "",
                addChannel = BadgeChannel.HUAWEI
            )
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
            detailChannel = badge.channel
        )
    }

    fun updateDetailInput(
        title: String = _uiState.value.detailTitle,
        remark: String = _uiState.value.detailRemark,
        link: String = _uiState.value.detailLink,
        channel: BadgeChannel = _uiState.value.detailChannel
    ) {
        _uiState.value = _uiState.value.copy(
            detailTitle = title,
            detailRemark = remark,
            detailLink = link,
            detailChannel = channel
        )
    }

    fun saveBadgeUpdate() {
        val state = _uiState.value
        state.editingBadge?.let { originalBadge ->
            BadgeRepository.updateBadge(
                originalBadge.id,
                state.detailTitle,
                state.detailRemark,
                state.detailLink,
                state.detailChannel
            )
            exitEditMode()
        }
    }

    fun deleteBadge() {
        val state = _uiState.value
        state.editingBadge?.let {
            BadgeRepository.removeBadge(it.id)
            exitEditMode()
        }
    }

    fun exitEditMode() {
        _uiState.value = _uiState.value.copy(editingBadge = null)
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
}
