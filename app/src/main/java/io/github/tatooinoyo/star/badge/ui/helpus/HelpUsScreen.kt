package io.github.tatooinoyo.star.badge.ui.helpus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.ui.component.PrimaryOrangeButton
import io.github.tatooinoyo.star.badge.ui.component.SecondaryScreenHeader
import io.github.tatooinoyo.star.badge.ui.icons.BadgeIcons
import io.github.tatooinoyo.star.badge.ui.theme.BadgeTokens
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.ui.theme.PeachTheme
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import io.github.tatooinoyo.star.badge.ui.theme.TextSecondary
import io.github.tatooinoyo.star.badge.utils.BadgeFormatterUtils
import io.github.tatooinoyo.star.badge.utils.SkExtractor
import io.github.tatooinoyo.star.badge.utils.preset.PresetRemoteStore
import io.github.tatooinoyo.star.badge.utils.preset.PresetResolver
import io.github.tatooinoyo.star.badge.utils.preset.PresetSubmissionHelper
import kotlinx.coroutines.launch

@Composable
fun HelpUsScreen(
    badges: List<Badge>,
    onNavigateBack: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val pollUrl = "https://f.kdocs.cn/g/IGyZAOLU/"

    var isSubmitting by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        PresetRemoteStore.refresh(context.applicationContext, force = true)
        refreshKey++
    }

    val unrecordedBadges = remember(badges, refreshKey) {
        badges.filter { badge ->
            if (badge.link.isBlank()) return@filter false
            val skCode = SkExtractor.getSkFromLinkOrNull(badge.link) ?: return@filter false
            !PresetResolver.isRecorded(context, skCode)
        }
    }

    fun openQuestionnaireFallback() {
        val badgesInfo = BadgeFormatterUtils.formatUnrecordedBadges(context, unrecordedBadges)
        clipboardManager.setText(AnnotatedString(badgesInfo))
        android.widget.Toast.makeText(
            context,
            context.getString(R.string.copy_all_badges_success, unrecordedBadges.size),
            android.widget.Toast.LENGTH_SHORT,
        ).show()
        uriHandler.openUri(pollUrl)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
            .safeDrawingPadding(),
    ) {
        SecondaryScreenHeader(
            title = stringResource(R.string.help_us),
            onBack = onNavigateBack,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.help_us_description),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(BadgeTokens.privacyHintBackground)
                    .border(1.dp, BadgeTokens.privacyHintBorder, MaterialTheme.shapes.small)
                    .padding(12.dp),
            ) {
                Text(
                    text = "💡 ${stringResource(R.string.help_us_privacy_note)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (unrecordedBadges.isNotEmpty()) {
                PrimaryOrangeButton(
                    text = if (isSubmitting) {
                        stringResource(R.string.submit_badges_in_progress)
                    } else {
                        stringResource(R.string.submit_all_badges)
                    },
                    onClick = {
                        if (isSubmitting) return@PrimaryOrangeButton
                        isSubmitting = true
                        scope.launch {
                            val result = PresetSubmissionHelper.submitUnrecordedBadges(unrecordedBadges)
                            isSubmitting = false
                            if (result.isSuccess) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.submit_badges_success,
                                        result.getOrElse { 0 },
                                    ),
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.submit_badges_failed,
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                                openQuestionnaireFallback()
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    icon = BadgeIcons.Share,
                )

                TextButton(
                    onClick = { openQuestionnaireFallback() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.copy_all_badges))
                }
            }

            if (unrecordedBadges.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(BadgeTokens.iconContainerBackground),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(BadgeIcons.IdCard),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = BrandOrange,
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.no_unrecorded_badges),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.help_us_all_recorded_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(unrecordedBadges) { badge ->
                        UnrecordedBadgeItem(
                            badge = badge,
                            onCopySkCode = { skCode ->
                                clipboardManager.setText(AnnotatedString(skCode))
                            },
                        )
                        HorizontalDivider(color = BorderDefault)
                    }
                }
            }
        }
    }
}

@Composable
fun UnrecordedBadgeItem(
    badge: Badge,
    onCopySkCode: (String) -> Unit,
) {
    val skCode = SkExtractor.getSkFromLinkOrNull(badge.link).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Text(
            text = badge.title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (badge.remark.isNotBlank()) {
            Text(
                text = badge.remark,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.sk_code, skCode),
                style = MaterialTheme.typography.bodyMedium,
                color = BrandOrange,
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = { onCopySkCode(skCode) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(stringResource(R.string.copy))
            }
        }
    }
}
