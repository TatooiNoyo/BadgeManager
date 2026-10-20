package io.github.tatooinoyo.star.badge.utils.preset

import io.github.tatooinoyo.star.badge.BuildConfig
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.utils.SkExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class BadgeSubmissionResult(
    val submittedCount: Int,
    val failedBadges: List<Badge>,
) {
    val isComplete: Boolean get() = submittedCount > 0 && failedBadges.isEmpty()
}

object PresetSubmissionHelper {
    private val client by lazy { PresetApiClient(BuildConfig.PRESET_API_URL) }

    suspend fun submitUnrecordedBadges(badges: List<Badge>): BadgeSubmissionResult =
        withContext(Dispatchers.IO) {
            submitUnrecordedBadges(badges, client::submitSubmission)
        }

    internal suspend fun submitUnrecordedBadges(
        badges: List<Badge>,
        submit: suspend (SubmissionRequest) -> Result<Unit>,
    ): BadgeSubmissionResult {
        var submitted = 0
        val failed = mutableListOf<Badge>()
        for (badge in badges) {
            currentCoroutineContext().ensureActive()
            val sk = SkExtractor.getSkFromLinkOrNull(badge.link)?.trim().orEmpty()
            val title = badge.title.trim()
            if (!sk.startsWith("SKY-", ignoreCase = true) || title.isBlank()) {
                failed += badge
                continue
            }
            val result = try {
                submit(
                    SubmissionRequest(
                        sk = sk.uppercase(),
                        title = title,
                        remark = badge.remark.trim(),
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            val error = result.exceptionOrNull()
            if (error is CancellationException) throw error
            if (result.isSuccess) {
                submitted++
            } else {
                failed += badge
            }
        }
        return BadgeSubmissionResult(submitted, failed)
    }
}
