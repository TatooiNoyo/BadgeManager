package io.github.tatooinoyo.star.badge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface BadgeDao {
    // 获取所有徽章，返回 Flow 实现实时更新
    @Query("SELECT * FROM badges order by orderIndex asc")
    fun getAllBadges(): Flow<List<Badge>>

    @Query("SELECT * FROM badges")
    suspend fun getAllBadges4export(): List<Badge>

    // 插入或替换
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    // 更新
    @Update
    suspend fun updateBadge(badge: Badge)

    // 批量更新
    @Update
    suspend fun updateBadges(badges: List<Badge>)

    // 根据 ID 更新
    @Query("UPDATE badges SET title = :title, remark = :remark, link = :link,channel = :channelName WHERE id = :id")
    suspend fun updateBadgeContent(
        id: String,
        title: String,
        remark: String,
        link: String,
        channelName: String
    )

    // 根据 ID 删除
    @Query("DELETE FROM badges WHERE id = :id")
    suspend fun deleteBadgeById(id: String)

    // 清除所有数据
    @Query("DELETE FROM badges")
    suspend fun deleteAll()

    // 批量插入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(badges: List<Badge>)

    @Transaction
    suspend fun replaceAll(badges: List<Badge>) {
        deleteAll()
        insertAll(badges)
    }

    @Query("SELECT MAX(orderIndex) FROM badges")
    suspend fun maxOrder(): Int?

    @Transaction
    suspend fun appendBadge(badge: Badge) {
        // 排序号读取与插入必须在同一事务中，避免并发新增分配到相同序号。
        val start = maxOf(0, maxOrder() ?: 0)
        require(start < Int.MAX_VALUE) { "Badge order is full" }
        insertBadge(badge.copy(orderIndex = start + 1))
    }

    @Transaction
    suspend fun appendCopies(badges: List<Badge>) {
        val start = maxOf(0, maxOrder() ?: 0)
        require(start <= Int.MAX_VALUE - badges.size) { "Badge order is full" }
        insertAll(badges.mapIndexed { index, badge ->
            badge.copy(id = UUID.randomUUID().toString(), orderIndex = start + index + 1)
        })
    }
}
