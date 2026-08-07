package io.github.tatooinoyo.star.badge.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = SurfaceWhite,
    primaryContainer = BrandOrangeLight,
    onPrimaryContainer = BrandOrange,
    secondary = BrandOrange,
    onSecondary = SurfaceWhite,
    tertiary = BrandOrange,
    onTertiary = SurfaceWhite,
    background = BackgroundStart,
    onBackground = TextPrimary,
    surface = SurfacePanel,
    onSurface = TextPrimary,
    surfaceContainerLowest = SurfaceWhite,
    onSurfaceVariant = TextSecondary,
    outline = BorderDefault,
    outlineVariant = BorderDefault,
    error = BrandOrange,
    onError = SurfaceWhite,
)

val PeachTheme = Brush.verticalGradient(
    colors = listOf(BackgroundStart, BackgroundEnd),
)

@Composable
fun BadgeManagerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = BadgeShapes,
        content = content,
    )
}
