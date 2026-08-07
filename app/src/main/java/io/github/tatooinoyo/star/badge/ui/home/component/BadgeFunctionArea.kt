package io.github.tatooinoyo.star.badge.ui.home.component

import android.content.Context
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.BadgeChannel
import io.github.tatooinoyo.star.badge.ui.component.BadgeMenuButton
import io.github.tatooinoyo.star.badge.ui.component.CapsuleTab
import io.github.tatooinoyo.star.badge.ui.home.BadgeUiState
import io.github.tatooinoyo.star.badge.ui.icons.BadgeIcons
import io.github.tatooinoyo.star.badge.ui.state.SyncState
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import kotlinx.coroutines.launch

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
    onToggleExpanded: () -> Unit,
    onTagsChange: (List<String>) -> Unit,
    onStartSender: () -> Unit,
    onStopSender: () -> Unit,
    onStartReceiver: (String) -> Unit,
    onStopReceiver: () -> Unit,
    onConfirmImport: () -> Unit = {},
    onCancelImport: () -> Unit = {},
    onImport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    onExport: (Context, Uri, (Boolean) -> Unit) -> Unit,
    allBadges: List<io.github.tatooinoyo.star.badge.data.Badge> = emptyList(),
    onShareSelectBadges: () -> Unit = {},
    onShareExport: () -> Unit = {},
    onShareFormatChange: (io.github.tatooinoyo.star.badge.utils.export.BadgeShareFormat) -> Unit = {},
    onCopyShareCode: (String) -> Unit = {},
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onUnrecordedBadgesClick: () -> Unit,
) {
    val tabs = listOf(
        stringResource(R.string.tab_input),
        stringResource(R.string.tab_backup),
        stringResource(R.string.tab_share),
        stringResource(R.string.tab_syncdata),
    )
    val pagerState = rememberPagerState(
        initialPage = uiState.functionTabIndex.coerceIn(0, tabs.lastIndex),
        pageCount = { tabs.size },
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.functionTabIndex) {
        val target = uiState.functionTabIndex.coerceIn(0, tabs.lastIndex)
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(target)
        }
    }

    AnimatedVisibility(
        visible = uiState.isFunctionAreaExpanded,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    tabs.forEachIndexed { index, title ->
                        CapsuleTab(
                            text = title,
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                MenuButton(
                    onSettingsClick = onSettingsClick,
                    onAboutClick = onAboutClick,
                    onUnrecordedBadgesClick = onUnrecordedBadgesClick,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (page) {
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
                        2 -> ShareBadgesPanel(
                            uiState = uiState,
                            allBadges = allBadges,
                            onSelectBadges = onShareSelectBadges,
                            onShareExport = onShareExport,
                            onShareFormatChange = onShareFormatChange,
                            onCopyCode = onCopyShareCode,
                        )
                        3 -> SyncDataPanel(
                            syncState = syncState,
                            onStartSender = onStartSender,
                            onStopSender = onStopSender,
                            onStartReceiver = onStartReceiver,
                            onStopReceiver = onStopReceiver,
                            onConfirmImport = onConfirmImport,
                            onCancelImport = onCancelImport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuButton(
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onUnrecordedBadgesClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        BadgeMenuButton(onClick = { expanded = true })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(R.string.settings),
                        color = TextPrimary,
                    )
                },
                onClick = {
                    expanded = false
                    onSettingsClick()
                },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(BadgeIcons.Settings),
                                contentDescription = null,
                                tint = TextPrimary,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.help_us),
                                color = TextPrimary,
                            )
                        },
                        onClick = {
                            expanded = false
                            onUnrecordedBadgesClick()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(BadgeIcons.Heart),
                                contentDescription = null,
                                tint = TextPrimary,
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.about),
                                color = TextPrimary,
                            )
                        },
                        onClick = {
                            expanded = false
                            onAboutClick()
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(BadgeIcons.Info),
                                contentDescription = null,
                                tint = TextPrimary,
                            )
                        },
                    )
        }
    }
}
