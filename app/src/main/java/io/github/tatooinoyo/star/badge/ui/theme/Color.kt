package io.github.tatooinoyo.star.badge.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette (from Figma)
val BrandOrange = Color(0xFFF26A1B)
val BrandOrangeLight = Color(0xFFFFECE0)
val BackgroundStart = Color(0xFFFFEFE2)
val BackgroundEnd = Color(0xFFFED8C3)
val SurfacePanel = Color(0xFFFFF6F0)
val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF2F2520)
val TextSecondary = Color(0xFF8C7B74)
val BorderDefault = Color(0xFFEAD6C9)
val BadgeCardBackground = Color(0xB3FFFFFF) // white 70%
val WarningOrange = Color(0xFFF26A1B)
val SuccessGreen = Color(0xFF4CAF50)

object BadgeTokens {
    val badgeCardBackground = BadgeCardBackground
    val iconContainerBackground = BrandOrangeLight
    val categoryTagBackground = BrandOrangeLight
    val categoryTagForeground = BrandOrange
    val serverTagBackground = SurfaceWhite
    val serverTagForeground = TextSecondary
    val filterChipSelectedBackground = BrandOrangeLight
    val filterChipSelectedBorder = BrandOrange
    val filterChipSelectedForeground = BrandOrange
    val filterChipUnselectedBackground = SurfaceWhite
    val filterChipUnselectedBorder = BorderDefault
    val filterChipUnselectedForeground = TextPrimary
    val privacyHintBackground = BrandOrangeLight.copy(alpha = 0.5f)
    val privacyHintBorder = BrandOrange.copy(alpha = 0.3f)
    val menuButtonBackground = SurfaceWhite.copy(alpha = 0.85f)
    val backButtonBackground = SurfaceWhite.copy(alpha = 0.9f)
}
