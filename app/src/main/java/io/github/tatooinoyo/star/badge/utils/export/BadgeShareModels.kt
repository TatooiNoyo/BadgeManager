package io.github.tatooinoyo.star.badge.utils.export

import io.github.tatooinoyo.star.badge.data.Badge

/** Plaintext JSON envelope stored inside the encrypted .badgeenc payload. */
data class BadgeShareEnvelope(
    val format: String = FORMAT_NAME,
    val formatVersion: Int = FORMAT_VERSION,
    val exportedAt: Long = System.currentTimeMillis(),
    val badges: List<Badge> = emptyList(),
) {
    companion object {
        const val FORMAT_NAME = "badge-share"
        const val FORMAT_VERSION = 1
    }
}

enum class BadgeShareFormat {
    TEXT,
    ENCRYPTED_FILE,
}

sealed class BadgeShareError : Exception() {
    data object InvalidFile : BadgeShareError()
    data object UnsupportedVersion : BadgeShareError()
    data object WrongPassword : BadgeShareError()
    data object EmptyPayload : BadgeShareError()
}
