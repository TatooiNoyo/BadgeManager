package io.github.tatooinoyo.star.badge.service.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FloatingWidget(
    onClick: () -> Unit,
) {
    // 获取当前配置以判断屏幕方向
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp


    // 动态计算根容器高度：横屏时铺满
    val rootHeight = if (isLandscape) screenHeight else 80.dp


    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .width(24.dp)
            .height(rootHeight)
            .background(Color.Transparent)
            .offset(y = if (isLandscape) 0.dp else (-200).dp)
            // 在外层处理所有手势
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // 长按打开菜单
                        onClick()
                    }
                )
            }
    ) {
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF333333, name = "星光侧边条预览")
@Composable
fun FloatingWidgetStarPreview() {
    FloatingWidget(onClick = {})
}