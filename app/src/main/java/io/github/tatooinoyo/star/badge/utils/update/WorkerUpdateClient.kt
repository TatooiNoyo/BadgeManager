package io.github.tatooinoyo.star.badge.utils.update

import android.util.Log
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 通过 Cloudflare Worker 代理检查更新（不携带蒲公英账号 Key）。
 * 超时不宜过短：国内访问 workers.dev 常需数秒才能建连，过短会误判失败并回退 GitHub。
 */
class WorkerUpdateClient(
    private val baseUrl: String,
    private val connectTimeoutMs: Int = CONNECT_TIMEOUT_MS,
    private val readTimeoutMs: Int = READ_TIMEOUT_MS,
) {
    suspend fun check(
        buildVersion: String,
        buildVersionCode: Int,
    ): Result<RemoteUpdateInfo> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("update proxy URL empty"))
        }
        // 国内偶发 DNS/建连抖动：失败后短间隔再试一次
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val attemptResult = runCatching {
                requestOnce(buildVersion, buildVersionCode)
            }
            if (attemptResult.isSuccess) {
                return@withContext attemptResult
            }
            lastError = attemptResult.exceptionOrNull()
            val detail = when (val e = lastError) {
                is SocketTimeoutException -> "timeout: ${e.message}"
                else -> "${e?.javaClass?.simpleName}: ${e?.message}"
            }
            Log.w(TAG, "Worker attempt ${attempt + 1}/$MAX_ATTEMPTS failed: $detail")
            if (attempt < MAX_ATTEMPTS - 1) {
                Thread.sleep(400)
            }
        }
        Result.failure(lastError ?: IllegalStateException("Worker check failed"))
    }

    private fun requestOnce(buildVersion: String, buildVersionCode: Int): RemoteUpdateInfo {
        val version = URLEncoder.encode(buildVersion, StandardCharsets.UTF_8.name())
        val url = URL(
            "${baseUrl.trimEnd('/')}/check?version=$version&code=$buildVersionCode"
        )
        Log.d(TAG, "GET $url (connect=${connectTimeoutMs}ms read=${readTimeoutMs}ms)")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "BadgeManager-UpdateChecker")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                error("Worker HTTP $code: $body")
            }
            Log.d(TAG, "ok HTTP $code bodyLen=${body.length}")
            return parse(body, buildVersion)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(body: String, localVersion: String): RemoteUpdateInfo {
        val root = JsonParser.parseString(body).asJsonObject
        if (root.has("error") && !root.get("error").isJsonNull) {
            val err = root.get("error").asString
            if (err.isNotBlank()) error(err)
        }
        val versionName = root.get("versionName")?.asString.orEmpty()
            .ifBlank { localVersion }
        val versionCode = when {
            root.has("versionCode") && !root.get("versionCode").isJsonNull &&
                root.get("versionCode").isJsonPrimitive -> {
                val prim = root.get("versionCode").asJsonPrimitive
                when {
                    prim.isNumber -> prim.asInt
                    prim.isString -> prim.asString.toIntOrNull()
                    else -> null
                }
            }
            else -> null
        }
        val downloadUrl = root.get("downloadUrl")?.asString.orEmpty()
        if (downloadUrl.isBlank() && versionName.isBlank()) {
            error("Worker response incomplete")
        }
        val sourceName = root.get("source")?.asString.orEmpty()
        val source = when (sourceName.lowercase()) {
            "pgyer" -> UpdateSource.PGYER
            "github" -> UpdateSource.GITHUB
            else -> UpdateSource.PGYER
        }
        return RemoteUpdateInfo(
            versionName = versionName,
            versionCode = versionCode,
            downloadUrl = downloadUrl.ifBlank { "https://www.pgyer.com/badgemanager" },
            releaseNotes = root.get("releaseNotes")?.asString.orEmpty(),
            forceUpdate = root.get("forceUpdate")?.asBoolean == true,
            source = source,
        )
    }

    companion object {
        private const val TAG = "WorkerUpdateClient"
        private const val MAX_ATTEMPTS = 2
        /** 建连：自定义域名偶发慢，4s 容易误超时后回退 GitHub */
        const val CONNECT_TIMEOUT_MS = 12_000
        const val READ_TIMEOUT_MS = 15_000
    }
}
