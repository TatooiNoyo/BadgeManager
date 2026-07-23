package io.github.tatooinoyo.star.badge.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * App-local icons that are not in material-icons-core (avoids pulling in icons-extended).
 */
object BadgeIcons {
    val Label: ImageVector
        get() {
            if (_label != null) return _label!!
            _label = ImageVector.Builder(
                name = "Label",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).apply {
                // Material Design "Label" (filled) — price-tag shape with hole
                path(
                    fill = SolidColor(Color.Black),
                    pathFillType = PathFillType.NonZero
                ) {
                    moveTo(17.63f, 5.84f)
                    curveTo(17.27f, 5.33f, 16.67f, 5f, 16f, 5f)
                    lineTo(5f, 5.01f)
                    curveTo(3.9f, 5.01f, 3f, 5.9f, 3f, 7f)
                    verticalLineToRelative(10f)
                    curveToRelative(0f, 1.1f, 0.9f, 1.99f, 2f, 1.99f)
                    lineTo(16f, 19f)
                    curveToRelative(0.67f, 0f, 1.27f, -0.33f, 1.63f, -0.84f)
                    lineTo(22f, 12f)
                    lineTo(17.63f, 5.84f)
                    close()
                    moveTo(7f, 14f)
                    curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                    reflectiveCurveToRelative(0.9f, -2f, 2f, -2f)
                    reflectiveCurveToRelative(2f, 0.9f, 2f, 2f)
                    reflectiveCurveToRelative(-0.9f, 2f, -2f, 2f)
                    close()
                }
            }.build()
            return _label!!
        }

    private var _label: ImageVector? = null
}
