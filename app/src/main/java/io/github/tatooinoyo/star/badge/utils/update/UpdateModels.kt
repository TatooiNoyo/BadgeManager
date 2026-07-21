package io.github.tatooinoyo.star.badge.utils.update

enum class UpdateSource {
    GITHUB,
    PGYER,
}

data class RemoteUpdateInfo(
    val versionName: String,
    val versionCode: Int? = null,
    val downloadUrl: String,
    val releaseNotes: String = "",
    val forceUpdate: Boolean = false,
    val source: UpdateSource,
)

sealed class UpdateCheckResult {
    data class Available(val info: RemoteUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestVersion: String) : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}
