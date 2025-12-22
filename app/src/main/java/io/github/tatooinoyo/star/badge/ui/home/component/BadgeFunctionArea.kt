package io.github.tatooinoyo.star.badge.ui.home.component

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
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
    onToggleExpanded: () -> Unit, // 切换展开的回调
    onTagsChange: (List<String>) -> Unit,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onStartReceiver: (String) -> Unit,
    onStopReceiver: () -> Unit,
    onImport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onExport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onSettingsClick: () -> Unit, // 点击设置菜单项
    onAboutClick: () -> Unit, // 点击帮助菜单项
    onUnrecordedBadgesClick: () -> Unit // 点击"未录入徽章"菜单项
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        stringResource(R.string.tab_input),
        stringResource(R.string.tab_backup),
        stringResource(R.string.tab_syncdata)
    )
    // 可折叠的功能面板区域
    AnimatedVisibility(
        visible = uiState.isFunctionAreaExpanded,
        // 定义进入动画：仅垂直展开 + 渐显
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        // 定义退出动画：仅垂直收缩 + 渐隐
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            // Tab Row 和 帮助按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        edgePadding = 0.dp, // 这里的 padding 设为 0 保持左对齐
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 菜单按钮
                MenuButton(
                    onSettingsClick = onSettingsClick,
                    onAboutClick = onAboutClick,
                    onUnrecordedBadgesClick = onUnrecordedBadgesClick
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        onExtractSkClick = onExtractSkClick,
                        onTagsChange = onTagsChange,
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
    }

    // 折叠/展开 按钮
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpanded() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (uiState.isFunctionAreaExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (uiState.isFunctionAreaExpanded) stringResource(R.string.collapse) else stringResource(
                R.string.expand
            ),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MenuButton(
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onUnrecordedBadgesClick: () -> Unit  // 添加这一行
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = stringResource(R.string.menu),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.settings)) },
                onClick = {
                    expanded = false
                    onSettingsClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.help_us)) },
                onClick = {
                    expanded = false
                    onUnrecordedBadgesClick()  // 添加这一行
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null
                    )
                }
            )

            DropdownMenuItem(
                text = { Text(stringResource(R.string.about)) },
                onClick = {
                    expanded = false
                    onAboutClick()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null
                    )
                }
            )
        }
    }
}
