package io.github.tatooinoyo.star.badge.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import io.github.tatooinoyo.star.badge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


// 定义预设的渠道
enum class BadgeChannel(val labelResId: Int, val packageName: String, val className: String) {
    HUAWEI(R.string.channel_huawei, "com.netease.sky.huawei", "com.tgc.sky.netease.GameActivity_Netease"),
    BILIBILI(R.string.channel_bilibili, "com.netease.sky.bilibili", "com.tgc.sky.netease.GameActivity_Netease"),
    VIVO(R.string.channel_vivo, "com.netease.sky.vivo", "com.tgc.sky.netease.GameActivity_Netease"),
    NETEASE(R.string.channel_netease, "com.netease.sky", "com.tgc.sky.netease.GameActivity_Netease"),
    SKY(R.string.channel_sky, "com.tgc.sky.android", "com.tgc.sky.GameActivity");

    fun getLabel(context: Context): String {
        return context.getString(labelResId)
    }
}

// 1. 徽章数据模型
@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val remark: String,
    val link: String = "",
    val channel: BadgeChannel = BadgeChannel.NETEASE,
    val orderIndex: Int,
    // 新增：标签列表
    val tags: List<String> = emptyList()
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
                context.getString(R.string.launch_failed, e.message),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}

// 2. 徽章仓库 (单例，全局共享)
object BadgeRepository {
    private var database: AppDatabase? = null
    private var badgeDao: BadgeDao? = null

    // 仅用于监听 DB → StateFlow；写操作由调用方 viewModelScope 驱动
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 内存缓存流，UI 监听这个
    private val _badges = MutableStateFlow<List<Badge>>(emptyList())
    val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    // 【必须】在 Application 或 MainActivity onCreate 中尽早调用
    fun initialize(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
            badgeDao = database?.badgeDao()

            scope.launch {
                badgeDao?.getAllBadges()?.collectLatest { dbList ->
                    _badges.value = dbList
                }
            }
        }
    }

    private fun requireDao(): BadgeDao {
        return badgeDao ?: error("BadgeRepository is not initialized")
    }

    suspend fun addBadge(
        title: String,
        remark: String,
        link: String,
        channel: BadgeChannel,
        tags: List<String> = emptyList()
    ) {
        val currentMaxOrder = _badges.value.maxOfOrNull { it.orderIndex } ?: 0
        val newBadge = Badge(
            title = title,
            remark = remark,
            link = link,
            channel = channel,
            tags = tags,
            orderIndex = currentMaxOrder + 1
        )
        requireDao().insertBadge(newBadge)
    }

    suspend fun updateBadge(
        id: String,
        title: String,
        remark: String,
        link: String,
        channel: BadgeChannel,
        orderIndex: Int,
        tags: List<String>
    ) {
        val badge = Badge(id, title, remark, link, channel, orderIndex, tags)
        requireDao().updateBadge(badge)
    }

    suspend fun updateBadgeOrder(badges: List<Badge>) {
        requireDao().updateBadges(badges)
    }

    suspend fun removeBadge(id: String) {
        requireDao().deleteBadgeById(id)
    }

    suspend fun getAllBadgesSnapshot(): List<Badge> {
        return requireDao().getAllBadges4export()
    }

    // 还原数据：清空旧数据并插入新数据（覆盖策略）
    suspend fun restoreBadges(badges: List<Badge>) {
        val dao = requireDao()
        dao.deleteAll()
        dao.insertAll(badges)
    }
}

class Converters {
    private val gson = Gson()
    private val tagsListType = object : TypeToken<List<String>>() {}.type

    @TypeConverter
    fun fromChannel(channel: BadgeChannel): String {
        return channel.name // 存枚举的名字，如 "HUAWEI"
    }

    @TypeConverter
    fun toChannel(value: String): BadgeChannel {
        return try {
            BadgeChannel.valueOf(value)
        } catch (e: Exception) {
            BadgeChannel.NETEASE // 默认值，防止枚举改名后崩溃
        }
    }

    @TypeConverter
    fun fromTagsList(tags: List<String>): String {
        val cleaned = tags.map { it.trim() }.filter { it.isNotEmpty() }
        return gson.toJson(cleaned)
    }

    @TypeConverter
    fun toTagsList(data: String): List<String> {
        if (data.isBlank()) return emptyList()
        val trimmed = data.trim()
        // 新格式：JSON 数组；旧格式：逗号分隔（兼容已有数据）
        if (trimmed.startsWith("[")) {
            return try {
                val list: List<String>? = gson.fromJson(trimmed, tagsListType)
                list?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
            } catch (_: Exception) {
                legacySplitTags(trimmed)
            }
        }
        return legacySplitTags(trimmed)
    }

    private fun legacySplitTags(data: String): List<String> {
        return data.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}