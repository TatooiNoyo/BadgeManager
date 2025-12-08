package io.github.tatooinoyo.star.badge.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// 1. 定义菜单项的数据模型 (替代 XML 中的 item)
data class DrawerMenuItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val remark: String,
    val onClick: () -> Unit
)


// 2. 菜单列表组件 (替代 NavigationView)
@Composable
fun DrawerMenu(
    items: List<DrawerMenuItem>,
    onGoHomeClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(vertical = 8.dp)
    ) {
        // === 新增：顶部关闭栏 ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp), // 稍微紧凑一点
            verticalAlignment = Alignment.CenterVertically,
            // 使用 SpaceBetween 让两个按钮分列左右两端
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            // 左边的主页按钮
            androidx.compose.material3.IconButton(
                onClick = onGoHomeClick,
                modifier = Modifier.size(32.dp) // 控制按钮大小
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "主页",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 右边的关闭按钮
            androidx.compose.material3.IconButton(
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
        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                // 【关键】限制最大高度。
                // 如果内容少，高度自适应；如果内容多，最大只有 400dp，超出可滚动。
                // 你可以根据需要调整 400.dp 这个数值
//                .heightIn(max = 400.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .clickable(onClick = item.onClick) // 处理点击事件
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp)) // 图标和文字的间距


        // 2. 右侧文字区域 (使用 Column 将 Title 和 Remark 上下排列)
        Column(
            modifier = Modifier.weight(1f) // 占据剩余宽度，防止文字挤压图标
        ) {
            // 标题 (加粗或稍微大一点)
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 备注/描述 (字体小一点，颜色淡一点)
            if (item.remark.isNotEmpty()) {
                Spacer(modifier = Modifier.size(2.dp)) // 标题和备注的微小间距
                Text(
                    text = item.remark,
                    style = MaterialTheme.typography.bodySmall, // 使用更小的字号
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // 使用次级颜色
                    maxLines = 2, // 限制行数，防止太长 (可选)
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis // 超出显示省略号
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 200) // 模拟抽屉的常见宽度
@Composable
fun DrawerMenuPreview() {
    // 模拟数据
    val sampleItems = listOf(
        DrawerMenuItem("home", "主页", Icons.Default.Home, "矮小爱哭爱玩闹") {},
        DrawerMenuItem("settings", "设置", Icons.Default.Settings,"矮小爱哭爱玩闹") {},
        DrawerMenuItem("logout", "退出", Icons.Default.Close,"矮小爱哭爱玩闹") {}
    )

    DrawerMenu(items = sampleItems, onGoHomeClick = {}, onCloseClick = {})
}