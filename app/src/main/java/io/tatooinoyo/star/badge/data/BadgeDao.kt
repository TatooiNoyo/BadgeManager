package io.tatooinoyo.star.badge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {
    // 获取所有徽章，返回 Flow 实现实时更新
    @Query("SELECT * FROM badges")
    fun getAllBadges(): Flow<List<Badge>>

    // 插入或替换
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    // 更新
    @Update
    suspend fun updateBadge(badge: Badge)

    // 根据 ID 删除
    @Query("DELETE FROM badges WHERE id = :id")
    suspend fun deleteBadgeById(id: String)
}
