package io.github.tatooinoyo.star.badge.ui.home.component

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.ui.home.BadgeUiState
import io.github.tatooinoyo.star.badge.ui.state.SyncState

@Composable
fun BadgeFunctionArea(
    uiState: BadgeUiState,
    syncState: SyncState,
    onInputTitleChange: (String) -> Unit,
    onInputRemarkChange: (String) -> Unit,
    onInputLinkChange: (String) -> Unit,
    onInputChannelChange: (BadgeChannel) -> Unit,
    onFastModeChange: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onExtractSkClick: (String) -> Unit,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onStartReceiver: (String) -> Unit,
    onStopReceiver: () -> Unit,
    onImport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onExport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onHelpClick: () -> Unit // 新增回调：点击帮助按钮
) {
    var isAddAreaExpanded by remember { mutableStateOf(true) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.tab_input),
        stringResource(R.string.tab_backup),
        "同网互传"
    )

    Column {
        // Tab Row 和 帮助按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onHelpClick) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "帮助与关于",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 可折叠的功能面板区域
        AnimatedVisibility(visible = isAddAreaExpanded) {
            Box(modifier = Modifier.height(350.dp)) {
                when (selectedTabIndex) {
                    0 -> BadgeInputPanel(
                        uiState = uiState,
                        onInputTitleChange = onInputTitleChange,
                        onInputRemarkChange = onInputRemarkChange,
                        onInputLinkChange = onInputLinkChange,
                        onInputChannelChange = onInputChannelChange,
                        onFastModeChange = onFastModeChange,
                        onAddClick = onAddClick,
                        onExtractSkClick = onExtractSkClick
                    )

                    1 -> BackupRestorePanel(onImport = onImport, onExport = onExport)
                    2 -> SyncDataPanel(
                        syncState = syncState,
                        onStartSender = onStartSender,
                        onStopSender = onStopSender,
                        onStartReceiver = onStartReceiver,
                        onStopReceiver = onStopReceiver
                    )
                }
            }
        }

        // 折叠/展开 按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isAddAreaExpanded = !isAddAreaExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isAddAreaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isAddAreaExpanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
