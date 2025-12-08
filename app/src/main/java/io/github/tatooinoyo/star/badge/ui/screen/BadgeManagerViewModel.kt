package io.github.tatooinoyo.star.badge.ui.screen

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.Charset
import android.util.Base64 // Import Base64
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

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
    val detailChannel: BadgeChannel = BadgeChannel.HUAWEI,
    val isWritingNfc: Boolean = false, // 是否正在等待写入 NFC
    val extractedSk: String? = null // 提取出的 SK 编码
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
        _uiState.value = _uiState.value.copy(editingBadge = null, isWritingNfc = false, extractedSk = null)
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
        _uiState.value = _uiState.value.copy(isWritingNfc = true)
    }

    fun cancelWritingNfc() {
        _uiState.value = _uiState.value.copy(isWritingNfc = false)
    }

    fun writeNfcTag(tag: Tag, activity: Activity): Boolean {
        if (!_uiState.value.isWritingNfc) return false
        
        val linkToWrite = _uiState.value.detailLink
        if (linkToWrite.isBlank()) {
            activity.runOnUiThread {
                Toast.makeText(activity, "链接为空，无法写入", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        val ndef = Ndef.get(tag)
        if (ndef == null) {
             activity.runOnUiThread {
                Toast.makeText(activity, "NFC标签不支持NDEF格式", Toast.LENGTH_SHORT).show()
            }
            return false
        }

        try {
            ndef.connect()
            if (!ndef.isWritable) {
                 activity.runOnUiThread {
                    Toast.makeText(activity, "NFC标签只读", Toast.LENGTH_SHORT).show()
                }
                return false
            }

            val ndefRecord = NdefRecord.createUri(linkToWrite)
            val ndefMessage = NdefMessage(arrayOf(ndefRecord))

            if (ndef.maxSize < ndefMessage.toByteArray().size) {
                 activity.runOnUiThread {
                    Toast.makeText(activity, "NFC标签容量不足", Toast.LENGTH_SHORT).show()
                }
                return false
            }

            ndef.writeNdefMessage(ndefMessage)
            activity.runOnUiThread {
                Toast.makeText(activity, "写入成功", Toast.LENGTH_SHORT).show()
            }
            _uiState.value = _uiState.value.copy(isWritingNfc = false)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
             activity.runOnUiThread {
                Toast.makeText(activity, "写入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            return false
        } finally {
            try {
                ndef.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    // === SK 提取逻辑 ===
    fun extractSkFromLink(link: String) {
        viewModelScope.launch {
            val decodedSk = try {
                val uri = android.net.Uri.parse(link)
                val sParam = uri.getQueryParameter("s")

                if (sParam.isNullOrBlank()) {
                    "链接中未找到 's' 参数。"
                } else {
                    // Base64 解码
                    val decodedBytes = Base64.decode(sParam, Base64.URL_SAFE) // Sky 的链接通常是 URL_SAFE
                    val decodedString = String(decodedBytes, Charset.forName("UTF-8"))

                    // 从解码后的字符串中提取 sk 参数
                    val decodedUri = android.net.Uri.parse("dummy://host?" + decodedString)
                    decodedUri.getQueryParameter("sk") ?: "解码后未找到 'sk' 参数。"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                "提取失败: ${e.message}"
            }
            _uiState.value = _uiState.value.copy(extractedSk = decodedSk)
        }
    }

    fun dismissSkDialog() {
        _uiState.value = _uiState.value.copy(extractedSk = null)
    }

}