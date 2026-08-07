package io.github.tatooinoyo.star.badge.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.BuildConfig
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.ui.component.BadgeContentCard
import io.github.tatooinoyo.star.badge.ui.component.PrimaryOrangeButton
import io.github.tatooinoyo.star.badge.ui.component.SecondaryScreenHeader
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.ui.theme.PeachTheme
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import io.github.tatooinoyo.star.badge.ui.theme.TextSecondary
import io.github.tatooinoyo.star.badge.utils.update.UpdateCheckResult
import io.github.tatooinoyo.star.badge.utils.update.UpdateChecker
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val projectUrl = "https://github.com/tatooinoyo/BadgeManager"
    val issuesUrl = "$projectUrl/issues"
    val pollUrl = "https://f.wps.cn/g/RQq78MAA"
    val contactMail = "tatooi.noyo@outlook.com"

    val updateChecker = remember { UpdateChecker(context) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateLoading by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<UpdateCheckResult?>(null) }

    fun startUpdateCheck() {
        showUpdateDialog = true
        updateLoading = true
        updateResult = null
        scope.launch {
            updateResult = updateChecker.check(force = true)
            updateLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
            .safeDrawingPadding(),
    ) {
        SecondaryScreenHeader(
            title = stringResource(R.string.help_title),
            onBack = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BadgeContentCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.usage_help_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandOrange,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HelpBullet(stringResource(R.string.usage_help_entry))
                    HelpBullet(stringResource(R.string.usage_help_share))
                    HelpBullet(stringResource(R.string.usage_help_sync))
                    HelpBullet(stringResource(R.string.usage_help_floating_menu))
                    HelpBullet(stringResource(R.string.usage_help_landscape_only))
                }
            }

            BadgeContentCard {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    LinkRow("📦 ${stringResource(R.string.help_repo)}") {
                        uriHandler.openUri(projectUrl)
                    }
                    HorizontalDivider(color = BorderDefault)
                    LinkRow("💡 ${stringResource(R.string.help_feedback)}") {
                        uriHandler.openUri(issuesUrl)
                    }
                    HorizontalDivider(color = BorderDefault)
                    LinkRow("📝 ${stringResource(R.string.help_poll)}") {
                        uriHandler.openUri(pollUrl)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.badge_not_found_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            BadgeContentCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.contact_me),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            text = contactMail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }

        PrimaryOrangeButton(
            text = stringResource(R.string.check_update) + " (V${BuildConfig.VERSION_NAME})",
            onClick = { startUpdateCheck() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        )
    }

    if (showUpdateDialog) {
        UpdateCheckDialog(
            result = updateResult,
            loading = updateLoading,
            onDismiss = { showUpdateDialog = false },
            onDismissVersion = { updateChecker.dismissVersion(it) },
        )
    }
}

@Composable
private fun HelpBullet(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("•", color = BrandOrange, fontWeight = FontWeight.Bold)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = TextPrimary,
            textDecoration = TextDecoration.Underline,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}
