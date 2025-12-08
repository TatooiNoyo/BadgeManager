package io.github.tatooinoyo.star.badge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Badge::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // 注册转换器
abstract class AppDatabase : RoomDatabase() {
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "badge_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
