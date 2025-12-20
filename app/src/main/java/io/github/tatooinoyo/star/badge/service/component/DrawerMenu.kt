package io.github.tatooinoyo.star.badge.service.component

import android.util.Log
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.data.BadgeChannel

// 1. 定义菜单项的数据模型 (替代 XML 中的 item)
data class DrawerMenuItem(
    val id: String,
    val title: String,
    val channel: BadgeChannel,
    val icon: ImageVector,
    val remark: String,
    val onClick: () -> Unit
)


// 2. 菜单列表组件 (替代 NavigationView)
@Composable
fun DrawerMenu(
    items: List<DrawerMenuItem>,
    lazyListState: LazyListState = rememberLazyListState(),
    allTags: List<String> = emptyList(),
    selectedTag: String? = null,
    onTagSelected: (String?) -> Unit = {},
    onGoHomeClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 控制下拉菜单展开状态
    var filterMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .pointerInput(Unit) {
                var totalDrag = 0f // 记录累计位移
                val threshold = 20.dp.toPx() // 将判定距离统一为 dp
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        Log.d("DrawerMenu", "onHorizontalDrag: $dragAmount")
                        change.consume()
                        totalDrag += dragAmount
                        if (totalDrag < -threshold) {
                            onCloseClick()
                            totalDrag = 0f
                        }
                    }
                )
            }
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp), // 稍微紧凑一点
            verticalAlignment = Alignment.CenterVertically,
            // 使用 SpaceBetween 让两个按钮分列左右两端
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左边的主页按钮
            IconButton(
                onClick = onGoHomeClick,
                modifier = Modifier.size(32.dp) // 控制按钮大小
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "主页",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 中间的标签筛选框
            if (allTags.isNotEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.weight(1f) // 占据中间剩余空间，确保居中
                ) {
                    FilterChip(
                        selected = selectedTag != null,
                        onClick = { filterMenuExpanded = true },
                        label = {
                            Text(
                                text = selectedTag ?: "全部标签",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        trailingIcon = {
                            Icon(
                                imageVector = if (filterMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        // 去掉边框，让它看起来更融入标题栏，或者保留边框增加辨识度
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent, // 平时透明
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                alpha = 0.5f
                            ),
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // 下拉菜单
                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false }
                    ) {
                        // 选项：全部
                        DropdownMenuItem(
                            text = { Text("全部标签") },
                            onClick = {
                                onTagSelected(null)
                                filterMenuExpanded = false
                            },
                            trailingIcon = if (selectedTag == null) {
                                {
                                    Icon(
                                        Icons.Default.Home,
                                        null,
                                        Modifier.size(16.dp)
                                    )
                                } // 借用 Home 图标表示默认
                            } else null
                        )
                        HorizontalDivider()
                        // 选项：各个标签
                        allTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    onTagSelected(tag)
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            } else {
                // 如果没有标签，使用 Spacer 占位保持布局平衡
                Spacer(modifier = Modifier.weight(1f))
            }

            // 右边的关闭按钮
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(32.dp) // 控制按钮大小
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭菜单",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 分割线 (可选，增加层次感)
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // 使用 items() 函数来批量加载数据
            items(items) { item ->
                DrawerMenuItemRow(item = item)
            }
        }
    }
}

// 3. 单个菜单项组件 (替代 XML 中的 <item>)
@Composable
fun DrawerMenuItemRow(item: DrawerMenuItem) {
    // 用于监听按压状态
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // 根据按压状态计算缩放比例 (按下缩小到 0.95，松开回弹到 1.0)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        label = "scaleAnimation"
    )

    Surface(
        onClick = item.onClick,
        // 将 interactionSource 传给 Surface 以便捕获按压状态
        interactionSource = interactionSource,
        // 设置圆角，使其看起来像药丸或圆角矩形
        shape = MaterialTheme.shapes.medium,
        // 使用 TonalElevation 自动生成与背景区分的颜色 (Material3 风格)
        tonalElevation = if (isPressed) 2.dp else 0.dp, // 按下时稍微加深颜色反馈
        // 设置默认背景色，使用 SurfaceVariant 让其比纯背景稍亮/暗，突出显示
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            // 外部间距：让每个 Item 之间分开
            .padding(start = 12.dp, end = 12.dp, top = 6.dp)
            .fillMaxWidth()
            // 应用缩放动画
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {

        // 内部内容布局
        Row(
            modifier = Modifier
                // 内部间距：让文字和图标不贴边
                .padding(horizontal = 16.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f) // 占据剩余宽度，防止文字挤压图标
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraSmall, // 胶囊圆角
                ) {
                    Text(
                        text = item.channel.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                // 标题 (加粗或稍微大一点)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, // 限制行数，防止太长 (可选)
                    overflow = TextOverflow.Ellipsis // 超出显示省略号
                )

                // 备注/描述 (字体小一点，颜色淡一点)
                if (item.remark.isNotEmpty()) {
                    Spacer(modifier = Modifier.size(2.dp)) // 标题和备注的微小间距
                    Text(
                        text = item.remark,
                        style = MaterialTheme.typography.bodySmall, // 使用更小的字号
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // 使用次级颜色
                        maxLines = 1, // 限制行数，防止太长 (可选)
                        overflow = TextOverflow.Ellipsis // 超出显示省略号
                    )
                } else {
                    // 关键修改：当没有备注时，渲染一个不可见的占位符，保持高度一致
                    Spacer(modifier = Modifier.size(2.dp))
                    Text(
                        text = " ", // 一个空格占位
                        style = MaterialTheme.typography.bodySmall,
                        // 也可以直接用 Modifier.alpha(0f) 隐藏
                    )
                }
            }
        }

    }
}

@Preview(showBackground = true, widthDp = 200) // 模拟抽屉的常见宽度
@Composable
fun DrawerMenuPreview() {
    // 模拟数据
    val sampleItems = listOf(
        DrawerMenuItem(
            "home",
            "主页",
            BadgeChannel.HUAWEI,
            Icons.Default.Home,
            "矮小爱哭爱玩闹"
        ) {},
        DrawerMenuItem(
            "settings",
            "设置",
            BadgeChannel.HUAWEI,
            Icons.Default.Settings,
            "矮小爱哭爱玩闹"
        ) {},
        DrawerMenuItem(
            "logout",
            "退出",
            BadgeChannel.HUAWEI,
            Icons.Default.Close,
            "矮小爱哭爱玩闹"
        ) {}
    )

    DrawerMenu(items = sampleItems, onGoHomeClick = {}, onCloseClick = {})
}