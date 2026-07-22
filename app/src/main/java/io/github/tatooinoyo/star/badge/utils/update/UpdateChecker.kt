package io.github.tatooinoyo.star.badge.utils.update

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import io.github.tatooinoyo.star.badge.BuildConfig
import io.github.tatooinoyo.star.badge.utils.LanguageManager
import io.github.tatooinoyo.star.badge.utils.LanguageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * 更新检查：
 * - 简体中文：Worker（蒲公英代理）→ 失败再 GitHub
 * - 其它语言：仅 GitHub
 *
 * 对话框里的「来源」取自最终成功的通道；若 Worker 失败回退到 GitHub，
 * 会带上 [UpdateCheckResult.Available.fellBackFromWorker]。
 */
class UpdateChecker(
    context: Context,
    private val worker: WorkerUpdateClient = WorkerUpdateClient(BuildConfig.UPDATE_PROXY_URL),
    private val github: GitHubUpdateClient = GitHubUpdateClient(BuildConfig.GITHUB_REPO),
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    suspend fun check(force: Boolean = false): UpdateCheckResult = withContext(Dispatchers.IO) {
        if (!force) {
            val cached = readCacheIfFresh()
            if (cached != null) return@withContext cached
        }

        val localVersion = BuildConfig.VERSION_NAME
        val localCode = BuildConfig.VERSION_CODE
        val errors = mutableListOf<String>()
        val preferWorker = prefersPgyerChannel(appContext)
        val channels = if (preferWorker) {
            listOf(Channel.WORKER, Channel.GITHUB)
        } else {
            listOf(Channel.GITHUB)
        }
        Log.d(TAG, "check channels=$channels force=$force proxy=${BuildConfig.UPDATE_PROXY_URL}")

        var workerFailed = false
        for (channel in channels) {
            val result = runChannel(channel, localVersion, localCode)
            when {
                result.isSuccess -> {
                    val info = result.getOrThrow()
                    val fellBack = preferWorker && channel == Channel.GITHUB && workerFailed
                    Log.i(
                        TAG,
                        "accepted channel=$channel source=${info.source} fellBack=$fellBack " +
                            "version=${info.versionName}"
                    )
                    val checkResult = toCheckResult(info, localVersion, localCode, fellBack)
                    writeCache(checkResult)
                    return@withContext checkResult
                }
                else -> {
                    val msg = result.exceptionOrNull()?.message ?: "unknown"
                    Log.w(TAG, "$channel failed: $msg")
                    errors += "$channel: $msg"
                    if (channel == Channel.WORKER) workerFailed = true
                }
            }
        }

        UpdateCheckResult.Failed(errors.joinToString("; ").ifBlank { "update check failed" })
    }

    /**
     * 静默检查：距上次成功检查不足 [minIntervalMs] 则跳过；仅当有新版本时返回 Available。
     */
    suspend fun checkSilently(minIntervalMs: Long = DEFAULT_SILENT_INTERVAL_MS): UpdateCheckResult? {
        val last = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L)
        if (System.currentTimeMillis() - last < minIntervalMs) return null
        return when (val result = check(force = false)) {
            is UpdateCheckResult.Available -> result
            is UpdateCheckResult.UpToDate -> {
                prefs.edit().putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis()).apply()
                null
            }
            is UpdateCheckResult.Failed -> null
        }
    }

    fun dismissVersion(versionName: String) {
        prefs.edit().putString(KEY_DISMISSED_VERSION, normalize(versionName)).apply()
    }

    fun isDismissed(versionName: String): Boolean {
        return prefs.getString(KEY_DISMISSED_VERSION, null) == normalize(versionName)
    }

    fun clearCache() {
        clearCache(appContext)
    }

    private suspend fun runChannel(
        channel: Channel,
        localVersion: String,
        localCode: Int,
    ): Result<RemoteUpdateInfo> {
        return when (channel) {
            Channel.WORKER -> worker.check(localVersion, localCode)
            Channel.GITHUB -> github.checkLatest()
        }
    }

    private fun toCheckResult(
        info: RemoteUpdateInfo,
        localVersion: String,
        localCode: Int,
        fellBackFromWorker: Boolean,
    ): UpdateCheckResult {
        val newerByName = VersionComparator.isNewer(info.versionName, localVersion)
        val newerByCode = info.versionCode != null && info.versionCode > localCode
        return if (newerByName || newerByCode) {
            UpdateCheckResult.Available(info, fellBackFromWorker = fellBackFromWorker)
        } else {
            UpdateCheckResult.UpToDate(
                currentVersion = localVersion,
                latestVersion = info.versionName,
                source = info.source,
                fellBackFromWorker = fellBackFromWorker,
            )
        }
    }

    private fun readCacheIfFresh(): UpdateCheckResult? {
        val at = prefs.getLong(KEY_LAST_SUCCESS_AT, 0L)
        if (System.currentTimeMillis() - at > CACHE_TTL_MS) return null
        val status = prefs.getString(KEY_CACHE_STATUS, null) ?: return null
        val latest = prefs.getString(KEY_CACHE_LATEST, null) ?: return null
        val download = prefs.getString(KEY_CACHE_DOWNLOAD, "") ?: ""
        val notes = prefs.getString(KEY_CACHE_NOTES, "") ?: ""
        val sourceName = prefs.getString(KEY_CACHE_SOURCE, UpdateSource.GITHUB.name)
        val source = runCatching { UpdateSource.valueOf(sourceName!!) }.getOrDefault(UpdateSource.GITHUB)
        val fellBack = prefs.getBoolean(KEY_CACHE_FALLBACK, false)
        val code = prefs.getInt(KEY_CACHE_CODE, -1).takeIf { it >= 0 }
        return when (status) {
            STATUS_AVAILABLE -> UpdateCheckResult.Available(
                RemoteUpdateInfo(
                    versionName = latest,
                    versionCode = code,
                    downloadUrl = download,
                    releaseNotes = notes,
                    source = source,
                ),
                fellBackFromWorker = fellBack,
            )
            STATUS_UP_TO_DATE -> UpdateCheckResult.UpToDate(
                currentVersion = BuildConfig.VERSION_NAME,
                latestVersion = latest,
                source = source,
                fellBackFromWorker = fellBack,
            )
            else -> null
        }
    }

    private fun writeCache(result: UpdateCheckResult) {
        val editor = prefs.edit().putLong(KEY_LAST_SUCCESS_AT, System.currentTimeMillis())
        when (result) {
            is UpdateCheckResult.Available -> {
                editor.putString(KEY_CACHE_STATUS, STATUS_AVAILABLE)
                    .putString(KEY_CACHE_LATEST, result.info.versionName)
                    .putString(KEY_CACHE_DOWNLOAD, result.info.downloadUrl)
                    .putString(KEY_CACHE_NOTES, result.info.releaseNotes)
                    .putString(KEY_CACHE_SOURCE, result.info.source.name)
                    .putBoolean(KEY_CACHE_FALLBACK, result.fellBackFromWorker)
                if (result.info.versionCode != null) {
                    editor.putInt(KEY_CACHE_CODE, result.info.versionCode)
                } else {
                    editor.remove(KEY_CACHE_CODE)
                }
            }
            is UpdateCheckResult.UpToDate -> {
                editor.putString(KEY_CACHE_STATUS, STATUS_UP_TO_DATE)
                    .putString(KEY_CACHE_LATEST, result.latestVersion)
                    .putString(KEY_CACHE_SOURCE, result.source.name)
                    .putBoolean(KEY_CACHE_FALLBACK, result.fellBackFromWorker)
                    .remove(KEY_CACHE_DOWNLOAD)
                    .remove(KEY_CACHE_NOTES)
            }
            is UpdateCheckResult.Failed -> return
        }
        editor.apply()
    }

    private fun normalize(version: String): String =
        version.trim().removePrefix("v").removePrefix("V")

    private enum class Channel { WORKER, GITHUB }

    companion object {
        private const val TAG = "UpdateChecker"
        private const val PREFS_NAME = "update_checker"
        private const val KEY_LAST_SUCCESS_AT = "last_success_at"
        private const val KEY_CACHE_STATUS = "cache_status"
        private const val KEY_CACHE_LATEST = "cache_latest"
        private const val KEY_CACHE_DOWNLOAD = "cache_download"
        private const val KEY_CACHE_NOTES = "cache_notes"
        private const val KEY_CACHE_SOURCE = "cache_source"
        private const val KEY_CACHE_CODE = "cache_code"
        private const val KEY_CACHE_FALLBACK = "cache_fallback"
        private const val KEY_DISMISSED_VERSION = "dismissed_version"
        private const val STATUS_AVAILABLE = "available"
        private const val STATUS_UP_TO_DATE = "up_to_date"
        private const val CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        const val DEFAULT_SILENT_INTERVAL_MS = 24 * 60 * 60 * 1000L

        /** 切换语言时清除检查结果与「稍后」记录，避免沿用错误通道的缓存 */
        fun clearCache(context: Context) {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        }

        /**
         * 简体中文走蒲公英 Worker；显式繁中/英文走 GitHub。
         * 跟随系统时按解析后的 Locale 判断是否为简体。
         */
        fun prefersPgyerChannel(context: Context): Boolean {
            val languageManager = LanguageManager.getInstance(context)
            val code = languageManager.getCurrentLanguage()
            if (code == LanguageUtils.LANGUAGE_CHINESE) return true
            if (code == LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL ||
                code == LanguageUtils.LANGUAGE_ENGLISH
            ) {
                return false
            }
            return isSimplifiedChineseLocale(languageManager.getLocale(code))
        }

        private fun isSimplifiedChineseLocale(locale: Locale): Boolean {
            if (locale.language != "zh") return false
            val country = locale.country.uppercase(Locale.ROOT)
            if (country in setOf("TW", "HK", "MO")) return false
            val tag = locale.toLanguageTag()
            if (tag.contains("Hant", ignoreCase = true)) return false
            return true
        }
    }
}
