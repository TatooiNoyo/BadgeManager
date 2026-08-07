package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.ui.component.BadgeContentCard
import io.github.tatooinoyo.star.badge.ui.component.FilterChipStyled
import io.github.tatooinoyo.star.badge.ui.component.PrimaryOrangeButton
import io.github.tatooinoyo.star.badge.ui.home.BadgeUiState
import io.github.tatooinoyo.star.badge.ui.icons.BadgeIcons
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareFormat

@Composable
fun ShareBadgesPanel(
    uiState: BadgeUiState,
    allBadges: List<Badge>,
    onSelectBadges: () -> Unit,
    onShareExport: () -> Unit,
    onShareFormatChange: (BadgeShareFormat) -> Unit,
    onCopyCode: (String) -> Unit,
) {
    val selectedBadges = allBadges.filter { it.id in uiState.shareSelectedIds }
    val scrollState = rememberScrollState()
    val isTextFormat = uiState.shareFormat == BadgeShareFormat.TEXT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BadgeContentCard {
            Text(
                text = stringResource(R.string.share_tab_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.share_format_label),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChipStyled(
                    label = stringResource(R.string.share_format_text),
                    selected = isTextFormat,
                    onClick = { onShareFormatChange(BadgeShareFormat.TEXT) },
                )
                FilterChipStyled(
                    label = stringResource(R.string.share_format_file),
                    selected = !isTextFormat,
                    onClick = { onShareFormatChange(BadgeShareFormat.ENCRYPTED_FILE) },
                )
            }
        }

        if (selectedBadges.isEmpty()) {
            PrimaryOrangeButton(
                text = stringResource(R.string.share_select_badges),
                onClick = onSelectBadges,
                icon = BadgeIcons.Share,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.share_selected_count, selectedBadges.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectBadges) {
                    Text(stringResource(R.string.share_reselect))
                }
            }

            BadgeContentCard {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    selectedBadges.take(4).forEach { badge ->
                        Text(
                            text = "• ${badge.title}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (selectedBadges.size > 4) {
                        Text(
                            text = stringResource(
                                R.string.share_selected_more,
                                selectedBadges.size - 4,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            PrimaryOrangeButton(
                text = stringResource(
                    if (isTextFormat) R.string.share_copy_to_clipboard
                    else R.string.share_generate_and_send,
                ),
                onClick = onShareExport,
                icon = BadgeIcons.Share,
            )
        }

        if (uiState.shareFormat == BadgeShareFormat.ENCRYPTED_FILE) {
            uiState.pendingShareCode?.let { code ->
                BadgeContentCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(R.string.share_verification_code),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 2.dp),
                        )
                        OutlinedButton(onClick = { onCopyCode(code) }) {
                            Icon(
                                painter = painterResource(BadgeIcons.Share),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.share_copy_code))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.share_send_code_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
