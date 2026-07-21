package io.github.tatooinoyo.star.badge.utils.update

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class GitHubUpdateClient(
    private val ownerRepo: String = "tatooinoyo/BadgeManager",
) {
    suspend fun checkLatest(): Result<RemoteUpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("https://api.github.com/repos/$ownerRepo/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "BadgeManager-UpdateChecker")
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
            connection.disconnect()
            if (code !in 200..299) {
                error("GitHub HTTP $code: $body")
            }
            parse(body)
        }
    }

    private fun parse(body: String): RemoteUpdateInfo {
        val root = JsonParser.parseString(body).asJsonObject
        val tag = root.get("tag_name")?.asString.orEmpty()
        if (tag.isBlank()) error("GitHub release missing tag_name")
        val htmlUrl = root.get("html_url")?.asString.orEmpty()
        val notes = root.get("body")?.asString.orEmpty()
        var apkUrl = ""
        root.getAsJsonArray("assets")?.forEach { el ->
            val asset = el.asJsonObject
            val name = asset.get("name")?.asString.orEmpty()
            val browserUrl = asset.get("browser_download_url")?.asString.orEmpty()
            if (name.endsWith(".apk", ignoreCase = true) && browserUrl.isNotBlank()) {
                apkUrl = browserUrl
                return@forEach
            }
        }
        val download = apkUrl.ifBlank { htmlUrl }
        if (download.isBlank()) error("GitHub release missing download URL")
        return RemoteUpdateInfo(
            versionName = tag,
            versionCode = null,
            downloadUrl = download,
            releaseNotes = notes,
            forceUpdate = false,
            source = UpdateSource.GITHUB,
        )
    }

    companion object {
        private const val TIMEOUT_MS = 8_000
    }
}
