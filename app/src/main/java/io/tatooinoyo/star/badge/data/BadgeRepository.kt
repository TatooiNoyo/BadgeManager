package io.tatooinoyo.star.badge.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

// 定义预设的渠道
enum class BadgeChannel(val label: String, val packageName: String, val className: String) {
    HUAWEI("华为", "com.netease.sky.huawei", "com.tgc.sky.netease.GameActivity_Netease"),
    BILIBILI("哔哩哔哩", "com.netease.sky.bilibili", "com.tgc.sky.netease.GameActivity_Netease"),
    VIVO("VIVO", "com.netease.sky.vivo", "com.tgc.sky.netease.GameActivity_Netease"),
    NETEASE("网易", "com.netease.sky", "com.tgc.sky.netease.GameActivity_Netease"),
    SKY("国际服", "com.tgc.sky.android", "com.tgc.sky.GameActivity")
}

// 1. 徽章数据模型
data class Badge(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val remark: String,
    val link: String = "",
    val channel: BadgeChannel = BadgeChannel.NETEASE
) {
    fun usage(context: Context) {
        try {
            // 对应 action: "android.nfc.action.NDEF_DISCOVERED"
            val intent = Intent("android.nfc.action.NDEF_DISCOVERED")

            // 对应 category: "android.intent.category.DEFAULT"
            intent.addCategory("android.intent.category.DEFAULT")

            // 对应 data: url (即 link 属性)
            // 注意：type: "https" 在 Android Intent 中通常指的是 data 的 scheme，
            // Uri.parse(link) 会自动解析出 https 协议头，无需显式 setType，
            // 显式 setType 通常用于设置 MIME 类型 (如 image/png)。
            if (link.isNotEmpty()) {
                intent.data = Uri.parse(link)
            }

            // 对应 packageName 和 className
            intent.setClassName(channel.packageName, channel.className)

            // 必须添加：因为可能从 Service (悬浮窗) 启动 Activity，需要加 NEW_TASK 标记
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // 执行启动
            context.startActivity(intent)

        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(
                context,
                "启动失败: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

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
    // 修改方法签名，增加 link 和 channel
    fun addBadge(title: String, remark: String, link: String, channel: BadgeChannel) {
        val newBadge = Badge(
            title = title,
            remark = remark,
            link = link,
            channel = channel
        )
        _badges.value = _badges.value + newBadge
    }

    // 在 BadgeRepository.kt 中添加
    fun updateBadge(
        id: String,
        title: String,
        remark: String,
        link: String,
        channel: BadgeChannel
    ) {
        _badges.value = _badges.value.map {
            if (it.id == id) {
                it.copy(title = title, remark = remark, link = link, channel = channel)
            } else {
                it
            }
        }
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
