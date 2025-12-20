package io.github.tatooinoyo.star.badge.service.component

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R

@Composable
fun FloatingWidget(
    isMenuOpen: Boolean,
    onClick: () -> Unit,
) {
    // 获取当前配置以判断屏幕方向
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeight = configuration.screenHeightDp.dp
    // 只有当 (内部认为可见) 且 (外部菜单没打开) 时，才真正显示
    val shouldShow = !isMenuOpen && !isLandscape
    // 透明度动画：显示时 1.0f，隐藏时 0.05f (保留一点点不透明度以确保容易调试位置，或者设为 0.01f)
    // 注意：如果完全为 0f，某些系统可能认为该窗口不可触摸，建议保留极小值或在 WindowManager 层处理
    val baseAlpha by animateFloatAsState(
        targetValue = if (shouldShow) 1.0f else 0f,
        label = "alpha"
    )


    // 动态计算根容器高度：横屏时铺满(或者很大)，竖屏时保持原有触摸区域大小
    val rootHeight = if (isLandscape) screenHeight else 80.dp

    // 动态计算视觉条高度：横屏时填满根容器，竖屏时变短
    val barHeight = if (isLandscape) screenHeight else 80.dp


    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .width(24.dp)
            .height(rootHeight)
            .background(Color.Transparent)
            .offset(y = if (isLandscape) 0.dp else (-200).dp)
            .pointerInput(isLandscape) {
                // 处理滑动
                if (isLandscape) {
                    var totalDrag = 0f
                    val threshold = 20.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                            Log.d("FloatingWidget", "onHorizontalDrag: $dragAmount")
                        },
                        onDragEnd = {
                            if (totalDrag < -threshold) {
                                onClick()
                            }
                            totalDrag = 0f
                        }
                    )
                }
            }
    ) {
        Box(
            contentAlignment = Alignment.CenterEnd,
            modifier = Modifier
                .width(24.dp)
                .alpha(baseAlpha) // 当它为0时，只是里面的画看不见了
        ) {
            // 胶囊条主体
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .width(6.dp) // 视觉上很细，像 Android 原生侧边条
                    .height(barHeight) // 稍微短一点，显得更精致
                    .shadow(
                        elevation = if (shouldShow) 4.dp else 0.dp,
                        shape = RoundedCornerShape(50),
                        spotColor = Color(0x40000000)
                    )
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF42A5F5), // 浅蓝
                                Color(0xFF1976D2)  // 深蓝
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    // 处理手势：点击显示/执行操作
                    .pointerInput(isLandscape) {
                        detectTapGestures(
                            onTap = {
                                if (!isLandscape) {
                                    // 如果已经是显示的，点击则触发业务逻辑
                                    onClick()
                                }
                            }
                        )
                    }
            ) {
                // 如果需要显示图标
                if (shouldShow) {
                    // 星星图标 (垂直居中)
                    Icon(
                        painter = painterResource(id = R.drawable.ic_floating_star),
                        contentDescription = "Handle",
                        tint = Color.White,
                        modifier = Modifier
                            .size(14.dp) // 小巧的图标
                    )
                }
            }

        }
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF333333, name = "星光侧边条预览")
@Composable
fun FloatingWidgetStarPreview() {
    FloatingWidget(onClick = {}, isMenuOpen = false)
}