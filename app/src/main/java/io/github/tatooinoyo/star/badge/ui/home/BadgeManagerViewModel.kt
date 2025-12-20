package io.github.tatooinoyo.star.badge.ui.home

import android.app.Activity
import android.app.Application
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Base64
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.data.BadgeRepository
import io.github.tatooinoyo.star.badge.data.PresetBadges
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.charset.Charset

// 定义 UI 状态
data class BadgeUiState(
    val badges: List<Badge> = emptyList(),
    val editingBadge: Badge? = null, // 如果不为空，则显示详情/编辑页
    // 添加模式下的输入状态
    val addTitle: String = "",
    val addRemark: String = "",
    val addLink: String = "",
    val addChannel: BadgeChannel = BadgeChannel.HUAWEI,
    val isFastMode: Boolean = false,
    // 详情模式下的输入状态 (如果是分离的页面，也可以拆分 ViewModel，这里为了简单先放一起)
    val detailTitle: String = "",
    val detailRemark: String = "",
    val detailLink: String = "",
    val detailChannel: BadgeChannel = BadgeChannel.HUAWEI,
    val detailTags: List<String> = emptyList(),
    val isWritingNfc: Boolean = false, // 是否正在等待写入 NFC
    val extractedSk: String? = null, // 提取出的 SK 编码
    val isFunctionAreaExpanded: Boolean = true, // 记录功能区是否展开
    val selectedTag: String? = null,
    val addTags: List<String> = emptyList(), // 当前新增徽章的标签
    val allTags: List<String> = emptyList()
)

// 定义一次性 UI 事件
sealed class BadgeUiEvent {
    data class ShowToast(val message: String) : BadgeUiEvent()
}

class BadgeManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BadgeUiState())
    val uiState: StateFlow<BadgeUiState> = _uiState.asStateFlow()

    // 创建事件流
    private val _uiEvent = MutableSharedFlow<BadgeUiEvent>()
    val uiEvent: SharedFlow<BadgeUiEvent> = _uiEvent.asSharedFlow()

    private val _selectedTag = MutableStateFlow<String?>(null) // null 代表显示全部

    init {
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

                // 返回一个数据类或 Triple 供 collect 使用
                Triple(filteredBadges, allTags, selectedTag)
            }.collect { (filteredBadges, allTags, currentTag) ->
                _uiState.update {
                    it.copy(
                        badges = filteredBadges, // UI 只展示过滤后的列表
                        allTags = allTags,       // 筛选栏展示所有可用标签
                        selectedTag = currentTag // 同步选中状态
                    )
                }
            }
        }
    }

    fun toggleFunctionArea() {
        _uiState.update { it.copy(isFunctionAreaExpanded = !it.isFunctionAreaExpanded) }
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
        channel: BadgeChannel = _uiState.value.addChannel,
        tags: List<String> = _uiState.value.addTags
    ) {
        val sk = getSkFromLink(link)
        val finalTitle = if (sk != null) PresetBadges.getTitle(getApplication<Application>(), sk) else title
        val finalRemark = if (sk != null) PresetBadges.getRemark(getApplication<Application>(), sk) else remark
        _uiState.value = _uiState.value.copy(
            addTitle = finalTitle,
            addRemark = finalRemark,
            addLink = link,
            addChannel = channel,
            addTags = tags // 确保更新状态
        )
        if (_uiState.value.isFastMode && link.isNotBlank() && sk != null) {
            addBadge()

            viewModelScope.launch {
                _uiEvent.emit(BadgeUiEvent.ShowToast("已自动添加: $finalTitle"))
            }
        }
    }

    fun toggleFastMode(enabled: Boolean) {
        _uiState.update { it.copy(isFastMode = enabled) }
    }

    fun addBadge() {
        val state = _uiState.value
        if (state.addTitle.isNotBlank()) {
            // 兼容没有协议的链接
            val finalLink = normalizeLink(state.addLink)

            BadgeRepository.addBadge(
                state.addTitle,
                state.addRemark,
                finalLink,
                state.addChannel,
                state.addTags
            )
            // 重置输入
            _uiState.value = state.copy(
                addTitle = "",
                addRemark = "",
                addLink = "",
                addTags = emptyList()
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
            detailChannel = badge.channel,
            detailTags = badge.tags
        )
    }

    fun updateDetailInput(
        title: String = _uiState.value.detailTitle,
        remark: String = _uiState.value.detailRemark,
        link: String = _uiState.value.detailLink,
        channel: BadgeChannel = _uiState.value.detailChannel,
        tags: List<String> = _uiState.value.detailTags
    ) {
        val sk = getSkFromLink(link)
        val finalTitle = if (sk != null) PresetBadges.getTitle(getApplication<Application>(), sk) else title
        val finalRemark = if (sk != null) PresetBadges.getRemark(getApplication<Application>(), sk) else remark
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
            val finalLink = normalizeLink(state.detailLink)
            BadgeRepository.updateBadge(
                originalBadge.id,
                state.detailTitle,
                state.detailRemark,
                finalLink,
                state.detailChannel,
                originalBadge.orderIndex,
                state.detailTags
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
        _uiState.value =
            _uiState.value.copy(editingBadge = null, isWritingNfc = false, extractedSk = null)
    }

    // === 拖拽排序 ===
    fun moveBadge(from: Int, to: Int) {
        val list = _uiState.value.badges.toMutableList()
        list.apply {
            add(to, removeAt(from))
        }

        // 更新内存中的 state，让 UI 立即响应
        _uiState.value = _uiState.value.copy(badges = list)
    }

    fun saveOrder() {
        val updatedList = _uiState.value.badges.mapIndexed { index, badge ->
            badge.copy(orderIndex = index)
        }
        BadgeRepository.updateBadgeOrder(updatedList)
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

    fun getSkFromLink(link: String): String {
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
        return decodedSk
    }

    // === SK 提取逻辑 ===
    fun extractSkFromLink(link: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(extractedSk = getSkFromLink(link))
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
            try {
                // 1. 获取数据快照
                val badges = BadgeRepository.getAllBadgesSnapshot()
                val gson =
                    com.google.gson.GsonBuilder().setPrettyPrinting().create() // 使用格式化输出，方便阅读
                val jsonString = gson.toJson(badges)

                // 2. 写入文件
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonString.toByteArray())
                }

                // 切回主线程通知结果
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false)
                }
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
                // 1. 读取文件内容
                val stringBuilder = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.io.BufferedReader(java.io.InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }
                val jsonString = stringBuilder.toString()

                if (jsonString.isBlank()) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false)
                    }
                    return@launch
                }

                // 2. 解析 JSON
                val gson = com.google.gson.Gson()
                val type = object : com.google.gson.reflect.TypeToken<List<Badge>>() {}.type
                val badges: List<Badge> = gson.fromJson(jsonString, type)

                // 3. 写入数据库
                if (badges.isNotEmpty()) {
                    BadgeRepository.restoreBadges(badges)
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true)
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

}