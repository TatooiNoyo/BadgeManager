package io.github.tatooinoyo.star.badge.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson

@Database(
    entities = [Badge::class],
    version = 3,
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

        /** 将逗号分隔的 tags 转为 JSON 数组，避免标签名含逗号时损坏。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                val gson = Gson()
                database.query("SELECT id, tags FROM badges").use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow("id")
                    val tagsIndex = cursor.getColumnIndexOrThrow("tags")
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIndex)
                        val raw = cursor.getString(tagsIndex).orEmpty().trim()
                        if (raw.isEmpty() || raw.startsWith("[")) continue

                        val tags = raw.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val json = gson.toJson(tags)
                        database.execSQL(
                            "UPDATE badges SET tags = ? WHERE id = ?",
                            arrayOf(json, id)
                        )
                    }
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "badge_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
