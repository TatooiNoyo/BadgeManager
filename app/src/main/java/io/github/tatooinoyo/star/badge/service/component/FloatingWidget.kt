package io.github.tatooinoyo.star.badge.service.component

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

@Composable
fun FloatingWidget(
    isMenuOpen: Boolean,
    onClick: () -> Unit
) {
    // 获取当前配置以判断屏幕方向
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    // 2. 获取屏幕高度 (DP)
    val screenHeight = configuration.screenHeightDp.dp
    // 控制显示状态：初始状态设为 true，确保刚启动时可见
    var isVisible by remember { mutableStateOf(true) }
    // 记录是否是首次启动（避免每次重组都触发10秒逻辑）
    var isFirstLaunch by remember { mutableStateOf(true) }

    // 只有当 (内部认为可见) 且 (外部菜单没打开) 时，才真正显示
    val shouldShow = isVisible && !isMenuOpen
    // 透明度动画：显示时 1.0f，隐藏时 0.05f (保留一点点不透明度以确保容易调试位置，或者设为 0.01f)
    // 注意：如果完全为 0f，某些系统可能认为该窗口不可触摸，建议保留极小值或在 WindowManager 层处理
    val baseAlpha by animateFloatAsState(targetValue = if (isVisible) 1.0f else 0f, label = "alpha")

    // 自动隐藏逻辑
    LaunchedEffect(isVisible) {
        if (isVisible) {
            // 如果是首次启动，保持显示 10 秒
            if (isFirstLaunch) {
                delay(5000)
                isFirstLaunch = false // 标记首次启动结束
            } else {
                // 如果是后续交互触发的显示，保持 3 秒
                delay(3000)
            }
            isVisible = false
        }
    }

    // 动态计算根容器高度：横屏时铺满(或者很大)，竖屏时保持原有触摸区域大小
    val rootHeight = if (isLandscape) screenHeight else 240.dp

    // 动态计算视觉条高度：横屏时填满根容器，竖屏时变短
    val barHeight = if (isLandscape) screenHeight else 140.dp // 竖屏改短为 140dp

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(12.dp)
            .height(rootHeight)
            .background(Color.Transparent)
            // 处理手势：点击显示/执行操作
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isVisible) {
                            // 如果当前是透明的，点击只是“唤醒”它
                            isVisible = true
                        } else {
                            // 如果已经是显示的，点击则触发业务逻辑
                            onClick()
                            isVisible = false
                        }
                    },
                    onLongPress = {
                        // 长按也可以唤醒
                        isVisible = true
                    }
                )
            }
            .pointerInput(Unit) {
                // 处理滑动
                detectHorizontalDragGestures { change, dragAmount ->
                    if (isMenuOpen) return@detectHorizontalDragGestures
                    change.consume()

                    // dragAmount > 0 向右滑, < 0 向左滑
                    if (dragAmount < 5) {
                        // 触发你的菜单展开逻辑
                        // 这里可能需要一个新的回调，例如 onSwipeOpen()
                        onClick() // 暂时复用点击逻辑
                        isVisible = false
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier.alpha(baseAlpha) // 当它为0时，只是里面的画看不见了
        ) {
            // 胶囊条主体
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
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