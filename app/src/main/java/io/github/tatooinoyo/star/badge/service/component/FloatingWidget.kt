package io.github.tatooinoyo.star.badge.service.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import kotlinx.coroutines.delay

@Composable
fun FloatingWidget(onClick: () -> Unit) {
    // 控制显示状态：初始状态设为 true，确保刚启动时可见
    var isVisible by remember { mutableStateOf(true) }
    // 记录是否是首次启动（避免每次重组都触发10秒逻辑）
    var isFirstLaunch by remember { mutableStateOf(true) }

    // 透明度动画：显示时 1.0f，隐藏时 0.05f (保留一点点不透明度以确保容易调试位置，或者设为 0.01f)
    // 注意：如果完全为 0f，某些系统可能认为该窗口不可触摸，建议保留极小值或在 WindowManager 层处理
    val baseAlpha by animateFloatAsState(targetValue = if (isVisible) 1.0f else 0.2f, label = "alpha")

    // --- 星光特效动画 ---
    val infiniteTransition = rememberInfiniteTransition(label = "starlight")

    // 1. 呼吸发光动画 (透明度 0.7 -> 1.0 循环)
    val starAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500), // 1.5秒一次呼吸
            repeatMode = RepeatMode.Reverse
        ),
        label = "starAlpha"
    )

    // 自动隐藏逻辑
    LaunchedEffect(isVisible) {
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

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(12.dp)
            .height(240.dp)
            .alpha(baseAlpha)
            // 2. 处理透明度
            .alpha(baseAlpha)
            // 3. 处理手势：点击显示/执行操作
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isVisible) {
                            // 如果当前是透明的，点击只是“唤醒”它
                            isVisible = true
                        } else {
                            // 如果已经是显示的，点击则触发业务逻辑 (或者你可以设计成双击触发)
                            onClick()
                        }
                    },
                    onLongPress = {
                        // 长按也可以唤醒
                        isVisible = true
                    }
                    // 注意：真正的“滑动”通常需要在 WindowManager 的 onTouchListener 中处理，
                    // 因为这涉及到改变悬浮窗在屏幕上的位置。
                    // 这里只处理组件内部的视觉响应。
                )
            }
            .pointerInput(Unit) {
                // 处理滑动 (新增逻辑)
                detectHorizontalDragGestures { change, dragAmount ->
                    // 1. 即使当前不可见，滑动也应该能触发（顺便唤醒它）
                    if (!isVisible) {
                        isVisible = true
                    }

                    // dragAmount > 0 向右滑, < 0 向左滑
                    // 假设悬浮球在左侧，向右滑(dragAmount > 0) 应该展开菜单
                    if (isVisible && dragAmount < 5) {
                        // 触发你的菜单展开逻辑
                        // 这里可能需要一个新的回调，例如 onSwipeOpen()
                        onClick() // 暂时复用点击逻辑
                        change.consume() // 消费事件
                    }
                }
            }
    ) {
        // 视觉主体（实际的长条）
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(8.dp) // 视觉宽度更细
                .height(240.dp)
                .shadow(
                    elevation = if (isVisible) 6.dp else 0.dp,
                    shape = RoundedCornerShape(50), // 胶囊形状
                    spotColor = Color(0x40000000)
                )
                .background(
                    brush = Brush.verticalGradient( // 垂直渐变
                        colors = listOf(
                            Color(0xFF64B5F6),
                            Color(0xFF2196F3)
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                )
        ) {
            // --- 视觉主体 (彻底去除硬边，纯光晕构建) ---

            if (isVisible) {
                // 1. 环境光晕 (Atmosphere) - 极度模糊，范围大，透明度低
                Box(
                    modifier = Modifier
                        .width(30.dp) // 宽度超出物理触摸区，制造溢出感
                        .height(260.dp)
                        .blur(32.dp) // 极大的模糊半径，彻底消除边缘
                        .background(
                            brush = Brush.radialGradient( // 改用径向渐变，比线性渐变更柔和
                                colors = listOf(
                                    Color(0x60FFD700), // 中心淡金
                                    Color.Transparent  // 边缘完全透明
                                ),
                                radius = 100f
                            ),
                            shape = CircleShape // 用圆形或椭圆发散
                        )
                        .alpha(starAlpha * 0.3f)
                )

                // 2. 核心光束 (Core) - 使用垂直渐变，但两端拉长透明区
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(220.dp)
                        .blur(12.dp) // 核心也增加模糊度
                        .background(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                0.2f to Color(0x20FFFFE0),
                                0.5f to Color(0xCCFFD54F), // 中心最亮
                                0.8f to Color(0x20FFFFE0),
                                1.0f to Color.Transparent
                            ),
                            shape = RoundedCornerShape(100)
                        )
                        .alpha(starAlpha * 0.7f)
                )

                // 3. 星星点缀 (Particles) - 保持锐利，形成强烈的虚实对比
                // 顶部星星
                StarParticle(Modifier.align(Alignment.TopCenter).offset(y = 60.dp), size = 3.dp, alpha = starAlpha)

                // 中间大星星(把手)
                Box(contentAlignment = Alignment.Center) {
                    // 图标背后的辉光
                    Box(modifier = Modifier.size(24.dp).blur(8.dp).background(Color(0xA0FFF9C4), CircleShape))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_floating_star),
                        contentDescription = "Handle",
                        tint = Color(0xFFFFFDE7),
                        modifier = Modifier.size(16.dp).alpha(starAlpha)
                    )
                }

                // 底部星星
                StarParticle(Modifier.align(Alignment.BottomCenter).offset(y = (-60).dp), size = 2.dp, alpha = starAlpha)
            }
        }
    }
}


// 辅助组件：简单的圆形粒子
@Composable
fun StarParticle(modifier: Modifier, size: androidx.compose.ui.unit.Dp, alpha: Float) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, CircleShape)
            .alpha(alpha)
            .shadow(4.dp, CircleShape, spotColor = Color.White)
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF333333, name = "星光侧边条预览")
@Composable
fun FloatingWidgetStarPreview() {
    FloatingWidget(onClick = {})
}