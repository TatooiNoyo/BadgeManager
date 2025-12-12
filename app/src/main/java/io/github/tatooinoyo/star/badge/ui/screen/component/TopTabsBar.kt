package io.github.tatooinoyo.star.badge.ui.screen.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import io.github.tatooinoyo.star.badge.ui.state.SyncState // 确保导入正确的包

@Composable
fun SyncTabContent(
    syncState: SyncState,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onStartReceiver: (String) -> Unit,
    onStopReceiver: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 使用 when 表达式根据状态渲染不同的 UI
        when (syncState) {

            // === 1. 空闲状态 (显示入口) ===
            is SyncState.Idle -> {
                SyncIdleView(
                    onStartSender = onStartSender,
                    onStartReceiver = onStartReceiver
                )
            }

            // === 2. 发送端状态 ===
            is SyncState.Sender -> {
                // 所有 Sender 状态都包含“停止分享”按钮，可以在外层统一加
                SenderStatusView(state = syncState)

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStopSender,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("停止分享")
                }
            }

            // === 3. 接收端状态 ===
            is SyncState.Receiver -> {
                ReceiverStatusView(state = syncState)

                Spacer(modifier = Modifier.height(16.dp))

                // 统一的底部按钮，根据状态改变文案和行为
                val (buttonText, buttonColor, buttonAction) = when (syncState) {
                    is SyncState.Receiver.Success -> Triple(
                        "完成",
                        MaterialTheme.colorScheme.primary,
                        onStopReceiver
                    )

                    is SyncState.Receiver.Error -> Triple(
                        "关闭 (失败)",
                        MaterialTheme.colorScheme.error,
                        onStopReceiver // 失败后点击也是重置回空闲状态，如果想重试需传入重试逻辑
                    )

                    else -> Triple(
                        "取消同步",
                        MaterialTheme.colorScheme.error,
                        onStopReceiver
                    )
                }

                Button(
                    onClick = buttonAction,
                    colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                    modifier = Modifier.fillMaxWidth(0.6f)
                ) {
                    Text(buttonText)
                }
            }
        }

    }
}

// --- 子组件示例 ---

@Composable
fun SyncIdleView(
    onStartSender: () -> Unit,
    onStartReceiver: (String) -> Unit
) {
    var inputCode by remember { mutableStateOf("") }

    // 发送入口
    OutlinedButton(onClick = onStartSender) {
        Icon(Icons.Default.Share, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("我是发送方 (生成码)")
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(24.dp))

    // 接收入口
    OutlinedTextField(
        value = inputCode,
        onValueChange = { if (it.length <= 6) inputCode = it },
        label = { Text("输入 6 位分享码") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = { onStartReceiver(inputCode) },
        enabled = inputCode.length == 6
    ) {
        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("我是接收方 (开始同步)")
    }
}

@Composable
fun SenderStatusView(state: SyncState.Sender) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is SyncState.Sender.Ready -> {
                    Text("您的分享码", style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = state.shareCode,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("等待接收端连接...", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                is SyncState.Sender.Handshaking -> {
                    Text("发现设备: ${state.targetIp}")
                    Text("正在建立安全连接...")
                    CircularProgressIndicator()
                }

                is SyncState.Sender.Sending -> {
                    Text("正在发送数据...")
                    LinearProgressIndicator()
                }

                is SyncState.Sender.Success -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                    Text("发送成功！")
                }

                is SyncState.Sender.Error -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "发送失败: ${state.message}",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text("代码: ${state.shareCode}")
                }
            }
        }
    }
}


@Composable
fun ReceiverStatusView(state: SyncState.Receiver) {
    // 这里只负责显示状态图标和文字提示，不再包含按钮
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when (state) {
            is SyncState.Receiver.Searching -> {
                Text("正在搜索发送端...")
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }

            is SyncState.Receiver.Receiving -> {
                Text("正在从 ${state.senderIp} 接收数据...")
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.8f))
            }

            is SyncState.Receiver.Success -> {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("同步成功！数据已覆盖。")
            }

            is SyncState.Receiver.Error -> {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "同步失败: ${state.message}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
