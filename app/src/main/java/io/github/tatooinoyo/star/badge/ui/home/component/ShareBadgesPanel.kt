package io.github.tatooinoyo.star.badge.ui.home.component

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.ui.home.BadgeUiState
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
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.share_tab_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 1) Format
        Text(
            text = stringResource(R.string.share_format_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = isTextFormat,
                onClick = { onShareFormatChange(BadgeShareFormat.TEXT) },
                label = {
                    Text(
                        text = stringResource(R.string.share_format_text),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = uiState.shareFormat == BadgeShareFormat.ENCRYPTED_FILE,
                onClick = { onShareFormatChange(BadgeShareFormat.ENCRYPTED_FILE) },
                label = {
                    Text(
                        text = stringResource(R.string.share_format_file),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            text = when (uiState.shareFormat) {
                BadgeShareFormat.TEXT -> stringResource(R.string.share_text_hint)
                BadgeShareFormat.ENCRYPTED_FILE -> stringResource(R.string.share_send_code_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

        // 2) Selection + action
        if (selectedBadges.isEmpty()) {
            Button(
                onClick = onSelectBadges,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.share_select_badges))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.share_selected_count, selectedBadges.size),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectBadges) {
                    Text(stringResource(R.string.share_reselect))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
            ) {
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

            Button(
                onClick = onShareExport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (isTextFormat) Icons.Default.ContentCopy else Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        if (isTextFormat) R.string.share_copy_to_clipboard
                        else R.string.share_generate_and_send
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 3) Verification code (encrypted only)
        if (uiState.shareFormat == BadgeShareFormat.ENCRYPTED_FILE) {
            uiState.pendingShareCode?.let { code ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ),
                ) {
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
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 2.dp),
                            letterSpacing = MaterialTheme.typography.headlineSmall.letterSpacing,
                        )
                        OutlinedButton(onClick = { onCopyCode(code) }) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
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
