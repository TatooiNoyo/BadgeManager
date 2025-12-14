package io.github.tatooinoyo.star.badge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Badge::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class) // 注册转换器
abstract class AppDatabase : RoomDatabase() {
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 这里的 SQL 必须匹配 TypeConverter 转换后的类型 (String -> TEXT)
                // DEFAULT '' 对应 emptyList() 转换后的空字符串
                database.execSQL("ALTER TABLE badges ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "badge_database"
                )
                    .addMigrations(MIGRATION_1_2) // 3. 应用迁移
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
