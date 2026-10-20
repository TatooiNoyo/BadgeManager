package io.github.tatooinoyo.star.badge

import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.utils.SkExtractor
import io.github.tatooinoyo.star.badge.utils.preset.PresetSubmissionHelper
import io.github.tatooinoyo.star.badge.utils.preset.SubmissionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class PresetSubmissionTest {
    private fun badge(index: Int) = Badge(
        id = "badge-$index", title = " Badge $index ", remark = " Remark $index ",
        link = SkExtractor.buildLinkFromSk("sky-test-$index"), orderIndex = index,
    )

    @Test fun allSuccessfulSubmissionsReportCompleteAndNormalizeRequests() = runBlocking {
        val requests = mutableListOf<SubmissionRequest>()
        val result = PresetSubmissionHelper.submitUnrecordedBadges(listOf(badge(1), badge(2))) {
            requests += it
            Result.success(Unit)
        }
        assertTrue(result.isComplete)
        assertEquals(2, result.submittedCount)
        assertTrue(result.failedBadges.isEmpty())
        assertEquals(SubmissionRequest("SKY-TEST-1", "Badge 1", "Remark 1"), requests.first())
    }

    @Test fun partialFailureKeepsOnlyFailedBadgesAndContinuesSubmitting() = runBlocking {
        val badges = (1..4).map(::badge)
        val requests = mutableListOf<String>()
        val result = PresetSubmissionHelper.submitUnrecordedBadges(badges) {
            requests += it.sk
            when (it.sk) {
                "SKY-TEST-2" -> Result.failure(IOException("HTTP 500"))
                "SKY-TEST-3" -> throw IOException("connection lost")
                else -> Result.success(Unit)
            }
        }
        assertFalse(result.isComplete)
        assertEquals(2, result.submittedCount)
        assertEquals(listOf(badges[1], badges[2]), result.failedBadges)
        assertEquals(4, requests.size)
    }

    @Test fun totalFailureKeepsEveryBadgeForFallback() = runBlocking {
        val badges = listOf(badge(1), badge(2))
        val result = PresetSubmissionHelper.submitUnrecordedBadges(badges) {
            Result.failure(IOException("offline"))
        }
        assertFalse(result.isComplete)
        assertEquals(0, result.submittedCount)
        assertEquals(badges, result.failedBadges)
    }

    @Test fun invalidBadgesAreNotSilentlyCountedAsComplete() = runBlocking {
        val valid = badge(1)
        val invalid = listOf(
            badge(2).copy(link = "invalid"), badge(3).copy(title = " "),
            badge(4).copy(link = SkExtractor.buildLinkFromSk("OTHER-4")),
        )
        var requests = 0
        val result = PresetSubmissionHelper.submitUnrecordedBadges(listOf(valid) + invalid) {
            requests++
            Result.success(Unit)
        }
        assertFalse(result.isComplete)
        assertEquals(1, requests)
        assertEquals(1, result.submittedCount)
        assertEquals(invalid, result.failedBadges)
    }

    @Test fun emptyBatchIsNotSuccessful() = runBlocking {
        val result = PresetSubmissionHelper.submitUnrecordedBadges(emptyList()) {
            fail("Empty batch must not send requests")
            Result.success(Unit)
        }
        assertFalse(result.isComplete)
        assertEquals(0, result.submittedCount)
        assertTrue(result.failedBadges.isEmpty())
    }

    @Test fun cancellationIsPropagatedAndStopsFurtherSubmissions() {
        for (returnAsFailure in listOf(false, true)) {
            var requests = 0
            assertThrows(CancellationException::class.java) {
                runBlocking {
                    PresetSubmissionHelper.submitUnrecordedBadges(listOf(badge(1), badge(2))) {
                        requests++
                        val cancellation = CancellationException("cancelled")
                        if (returnAsFailure) Result.failure(cancellation) else throw cancellation
                    }
                }
            }
            assertEquals(1, requests)
        }
    }
}
