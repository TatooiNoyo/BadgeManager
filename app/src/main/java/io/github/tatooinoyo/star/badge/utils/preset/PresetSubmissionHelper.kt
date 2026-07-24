package io.github.tatooinoyo.star.badge.utils.preset

import io.github.tatooinoyo.star.badge.BuildConfig
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.utils.SkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object PresetSubmissionHelper {
    private val client by lazy { PresetApiClient(BuildConfig.PRESET_API_URL) }

    suspend fun submitUnrecordedBadges(badges: List<Badge>): Result<Int> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.PRESET_API_URL.isBlank()) {
                return@withContext Result.failure(IllegalStateException("preset API URL empty"))
            }
            var submitted = 0
            var lastError: Throwable? = null
            for (badge in badges) {
                val sk = SkExtractor.getSkFromLinkOrNull(badge.link)?.trim().orEmpty()
                if (sk.isBlank() || !sk.startsWith("SKY-", ignoreCase = true)) continue
                val title = badge.title.trim()
                if (title.isBlank()) continue
                val result = client.submitSubmission(
                    SubmissionRequest(
                        sk = sk.uppercase(),
                        title = title,
                        remark = badge.remark.trim(),
                    )
                )
                if (result.isSuccess) {
                    submitted++
                } else {
                    lastError = result.exceptionOrNull()
                }
            }
            if (submitted > 0) {
                Result.success(submitted)
            } else {
                Result.failure(lastError ?: IllegalStateException("No badges submitted"))
            }
        }
}
