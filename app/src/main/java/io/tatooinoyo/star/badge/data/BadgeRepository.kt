package io.tatooinoyo.star.badge.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// 1. 徽章数据模型
data class Badge(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val remark: String
)

// 2. 徽章仓库 (单例，全局共享)
object BadgeRepository {
    // 初始模拟数据
    private val _badges = MutableStateFlow<List<Badge>>(
        listOf(
            Badge(title = "早起打卡", remark = "连续坚持 5 天"),
            Badge(title = "背单词", remark = "雅思核心词汇 List 1")
        )
    )

    // 公开的只读流，供 Service 和 Activity 监听
    val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    // 添加徽章
    fun addBadge(title: String, remark: String) {
        val newBadge = Badge(title = title, remark = remark)
        _badges.value = _badges.value + newBadge
    }

    // 删除徽章
    fun removeBadge(id: String) {
        _badges.value = _badges.value.filter { it.id != id }
    }

    // (可选) 更新徽章
    fun updateBadge(id: String, newTitle: String, newRemark: String) {
        _badges.value = _badges.value.map {
            if (it.id == id) it.copy(title = newTitle, remark = newRemark) else it
        }
    }
}
