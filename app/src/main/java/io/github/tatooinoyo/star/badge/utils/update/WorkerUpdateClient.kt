package io.github.tatooinoyo.star.badge.utils.update

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.net.URLEncoder

/**
 * 通过 Cloudflare Worker 代理检查更新（不携带蒲公英账号 Key）。
 * 短超时：国内访问 workers.dev 常失败，尽快回退到 GitHub。
 */
class WorkerUpdateClient(
    private val baseUrl: String,
    private val connectTimeoutMs: Int = SHORT_TIMEOUT_MS,
    private val readTimeoutMs: Int = SHORT_TIMEOUT_MS,
) {
    suspend fun check(
        buildVersion: String,
        buildVersionCode: Int,
    ): Result<RemoteUpdateInfo> = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) {
            return@withContext Result.failure(IllegalStateException("update proxy URL empty"))
        }
        runCatching {
            val version = URLEncoder.encode(buildVersion, "UTF-8")
            val url = URL(
                "${baseUrl.trimEnd('/')}/check?version=$version&code=$buildVersionCode"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "BadgeManager-UpdateChecker")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code !in 200..299) {
                error("Worker HTTP $code: $body")
            }
            parse(body, buildVersion)
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
            root.has("versionCode") && !root.get("versionCode").isJsonNull ->
                root.get("versionCode").asInt
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
        /** workers.dev 在国内常连不上，短超时以便快速回退 GitHub */
        const val SHORT_TIMEOUT_MS = 4_000
    }
}
