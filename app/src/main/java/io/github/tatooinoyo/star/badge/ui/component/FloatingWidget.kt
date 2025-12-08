package io.github.tatooinoyo.star.badge.ui.component


import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FloatingWidget(onClick: () -> Unit) {
    // 对应 XML 中的 CardView
    Card(
        onClick = onClick,
        modifier = Modifier, // layout_margin="4dp"
        shape = RoundedCornerShape(24.dp), // cardCornerRadius="24dp"
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp), // cardElevation="8dp"
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2196F3)) // cardBackgroundColor="#2196F3"
    ) {
        // 对应 XML 中的 LinearLayout (Compose 中用 Row 或 Box)
        Row(
            modifier = Modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 对应 XML 中的 ImageView
            Icon(
                imageVector = Icons.Default.Menu, // 或者是你之前的 ic_menu_directions 资源
                contentDescription = "Menu",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ==== 添加这个预览函数 ====
@Preview(showBackground = true, name = "悬浮球预览")
@Composable
fun FloatingWidgetPreview() {
    // 这里调用你的组件，并传入假的参数
    FloatingWidget(
        onClick = {}
    )
}