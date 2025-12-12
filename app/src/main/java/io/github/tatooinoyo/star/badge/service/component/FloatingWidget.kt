package io.github.tatooinoyo.star.badge.service.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R

@Composable
fun FloatingWidget(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(46.dp) // 增大尺寸以适应气泡效果
            .padding(4.dp) // 外边距
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                spotColor = Color(0x40000000),
                ambientColor = Color(0x40000000)
            )
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF64B5F6), // 浅蓝色中心
                        Color(0xFF2196F3)  // 深蓝色边缘
                    ),
                    center = Offset(30f, 30f), // 高光偏移
                    radius = 100f
                ),
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = Color(0xFFBBDEFB), // 亮色边框增加立体感
                shape = CircleShape
            )
            .clickable(
                onClick = onClick,
                indication = null, // 移除点击涟漪，避免破坏气泡感
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_floating_star),
            contentDescription = "Menu",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Preview(showBackground = false, name = "悬浮球预览")
@Composable
fun FloatingWidgetPreview() {
    FloatingWidget(onClick = {})
}