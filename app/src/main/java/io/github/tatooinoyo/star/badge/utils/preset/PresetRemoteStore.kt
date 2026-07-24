package io.github.tatooinoyo.star.badge.utils.preset

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.github.tatooinoyo.star.badge.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 缓存 Worker 下发的已审核候选徽章；App 启动或进入「帮助完善」时刷新。
 */
object PresetRemoteStore {
    private const val PREFS = "preset_remote_store"
    private const val KEY_JSON = "presets_json"
    private const val KEY_UPDATED_AT = "updated_at"

    private val gson = Gson()
    private val mutex = Mutex()
    private val client by lazy { PresetApiClient(BuildConfig.PRESET_API_URL) }

    @Volatile
    private var cache: Map<String, PresetItem> = emptyMap()

    @Volatile
    private var updatedAt: Long = 0L

    fun initialize(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_JSON, null) ?: return
        runCatching {
            val response = gson.fromJson(json, PresetsResponse::class.java) ?: return
            cache = response.items.associateBy { normalizeSk(it.sk) }
            updatedAt = prefs.getLong(KEY_UPDATED_AT, response.updatedAt)
        }.onFailure {
            Log.w(TAG, "Failed to load cached presets", it)
        }
    }

    suspend fun refresh(context: Context, force: Boolean = false): Result<Unit> =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                client.fetchPresets()
                    .mapCatching { response ->
                        if (!force && response.updatedAt > 0 && response.updatedAt <= updatedAt) {
                            return@mapCatching
                        }
                        val map = response.items.associateBy { normalizeSk(it.sk) }
                        cache = map
                        updatedAt = response.updatedAt
                        persist(context.applicationContext, response)
                    }
            }
        }

    fun getTitle(sk: String): String = cache[normalizeSk(sk)]?.title.orEmpty()

    fun getRemark(sk: String): String = cache[normalizeSk(sk)]?.remark.orEmpty()

    private fun persist(context: Context, response: PresetsResponse) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_JSON, gson.toJson(response))
            .putLong(KEY_UPDATED_AT, response.updatedAt)
            .apply()
    }

    private fun normalizeSk(sk: String): String = sk.trim().uppercase()

    private const val TAG = "PresetRemoteStore"
}
