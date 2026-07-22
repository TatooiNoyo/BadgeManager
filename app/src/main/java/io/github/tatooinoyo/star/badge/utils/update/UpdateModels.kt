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
    data class Available(
        val info: RemoteUpdateInfo,
        /** 本应优先蒲公英 Worker，但 Worker 失败后改用了 GitHub */
        val fellBackFromWorker: Boolean = false,
    ) : UpdateCheckResult()

    data class UpToDate(
        val currentVersion: String,
        val latestVersion: String,
        val source: UpdateSource,
        val fellBackFromWorker: Boolean = false,
    ) : UpdateCheckResult()

    data class Failed(val message: String) : UpdateCheckResult()
}
