package io.github.tatooinoyo.star.badge.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID


data class TitleAndRemark(val title: String, val remark: String)

// 内置的SK,title,remark关系
val PRESET_BADGES_MAP = mapOf(
    "SKY-PN-ST-POR-CP" to TitleAndRemark("预言山谷", "15s CD"),
    "SKY-PN-ST-SUM-SP" to TitleAndRemark("云野传说", "15s CD"),
    "SKY-KC-ST-COB-AI" to TitleAndRemark("光之爱钥匙扣", "3min CD, 3min Dur"),
    "SKY-KC-ST-COB-AU" to TitleAndRemark("欧若拉钥匙扣", "3min CD, 3min Dur"),
    "SKY-BK-ST-PRO-ART" to TitleAndRemark("光遇设定集", "15min CD, 20min Dur"),
    "SKY-KC-ST-LPP-TF" to TitleAndRemark("狐狸毛绒钥匙扣", "15min CD, 10min Dur"),
    "SKY-KC-ST-COB-MM" to TitleAndRemark("姆明玩偶钥匙扣", "15min CD, 10min Dur"),
    "SKY-UM-ST-PRO-LU" to TitleAndRemark("追光者雨伞", "15min CD, 10min Dur"),
    "SKY-PN-ST-PRO-LT" to TitleAndRemark("灯笼徽章", "10min CD, 30min Dur"),
    "SKY-FG-ST-PRO-FG-SF1" to TitleAndRemark("三周年小蓝", "15s CD"),
    "SKY-PN-ST-BL-TS" to TitleAndRemark("小不点", "15min CD, 20min Dur"),
    "SKY-PN-ST-BL-HS" to TitleAndRemark("大只佬", "15min CD, 20min Dur"),
    "SKY-PN-ST-MAS-CB" to TitleAndRemark("矮人徽章", "15min CD, 10min Dur"),
    "SKY-KC-ST-BL-GR" to TitleAndRemark("长大成人", "15min CD, 20min Dur"),
    "SKY-PN-ST-CAP-MB" to TitleAndRemark("鬼蝙蝠斗篷", "20min CD, 15min Dur"),
    "SKY-PN-ST-CNT-YIR" to TitleAndRemark("你我成双徽章(麻花款)", "15s CD"),
    "SKY-PN-ST-CNT-YIL" to TitleAndRemark("你我成双徽章(竖琴款)", "15s CD"),
    "SKY-PN-ST-PL-HC" to TitleAndRemark("叠叠蟹", "15min CD, 10min Dur"),
)

// 定义预设的渠道
enum class BadgeChannel(val label: String, val packageName: String, val className: String) {
    HUAWEI("华为", "com.netease.sky.huawei", "com.tgc.sky.netease.GameActivity_Netease"),
    BILIBILI("哔哩哔哩", "com.netease.sky.bilibili", "com.tgc.sky.netease.GameActivity_Netease"),
    VIVO("VIVO", "com.netease.sky.vivo", "com.tgc.sky.netease.GameActivity_Netease"),
    NETEASE("网易", "com.netease.sky", "com.tgc.sky.netease.GameActivity_Netease"),
    SKY("国际服", "com.tgc.sky.android", "com.tgc.sky.GameActivity")
}

// 1. 徽章数据模型
@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val remark: String,
    val link: String = "",
    val channel: BadgeChannel = BadgeChannel.NETEASE,
    val orderIndex: Int
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
    private var database: AppDatabase? = null
    private var badgeDao: BadgeDao? = null

    // 内存缓存流，UI 监听这个
    private val _badges = MutableStateFlow<List<Badge>>(emptyList())
    val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    // 【必须】在 Application 或 MainActivity onCreate 中尽早调用
    fun initialize(context: Context) {
        if (database == null) {
            database = AppDatabase.getDatabase(context)
            badgeDao = database?.badgeDao()

            // 启动协程监听数据库变化，并更新到 StateFlow
            CoroutineScope(Dispatchers.IO).launch {
                badgeDao?.getAllBadges()?.collectLatest { dbList ->
                    _badges.value = dbList
                }
            }
        }
    }

    // 添加徽章
    // 修改方法签名，增加 link 和 channel
    fun addBadge(title: String, remark: String, link: String, channel: BadgeChannel) {
        val currentMaxOrder = _badges.value.maxOfOrNull { it.orderIndex } ?: 0
        val newBadge = Badge(
            title = title,
            remark = remark,
            link = link,
            channel = channel,
            orderIndex = currentMaxOrder + 1
        )
        CoroutineScope(Dispatchers.IO).launch {
            badgeDao?.insertBadge(newBadge)
        }
    }

    // 在 BadgeRepository.kt 中添加
    fun updateBadge(
        id: String,
        title: String,
        remark: String,
        link: String,
        channel: BadgeChannel,
        orderIndex: Int // 保持 orderIndex 不变
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            // 这里应该调用 updateBadgeContent，但它没有 orderIndex，所以我们直接更新整个对象
            val badge = Badge(id, title, remark, link, channel, orderIndex)
            badgeDao?.updateBadge(badge)
        }
    }

    fun updateBadgeOrder(badges: List<Badge>) {
        CoroutineScope(Dispatchers.IO).launch {
            badgeDao?.updateBadges(badges)
        }
    }

    // 删除徽章
    fun removeBadge(id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            badgeDao?.deleteBadgeById(id)
        }
    }

    // 获取当前所有徽章的一次性快照（非 Flow），用于导出
    fun getAllBadgesSnapshot(): List<Badge> {
        return badgeDao?.getAllBadges4export() ?: emptyList()
    }

    // 还原数据：清空旧数据并插入新数据
    suspend fun restoreBadges(badges: List<Badge>) {
        // 这一步取决于你的策略：是“覆盖”还是“追加”？
        // 策略 A: 覆盖 (先清空再插入) - 推荐用于完整备份还原
        badgeDao?.deleteAll()
        badgeDao?.insertAll(badges)
    }
}

class Converters {
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
}