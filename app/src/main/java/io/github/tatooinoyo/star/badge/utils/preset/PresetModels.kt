package io.github.tatooinoyo.star.badge.utils.preset

data class PresetItem(
    val sk: String,
    val title: String,
    val remark: String = "",
)

data class PresetsResponse(
    val updatedAt: Long = 0L,
    val items: List<PresetItem> = emptyList(),
)

data class SubmissionRequest(
    val sk: String,
    val title: String,
    val remark: String = "",
)
