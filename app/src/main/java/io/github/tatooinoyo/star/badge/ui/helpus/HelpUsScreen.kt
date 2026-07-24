package io.github.tatooinoyo.star.badge.ui.helpus

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.utils.BadgeFormatterUtils
import io.github.tatooinoyo.star.badge.utils.SkExtractor
import io.github.tatooinoyo.star.badge.utils.preset.PresetRemoteStore
import io.github.tatooinoyo.star.badge.utils.preset.PresetResolver
import io.github.tatooinoyo.star.badge.utils.preset.PresetSubmissionHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpUsScreen(
    badges: List<Badge>,
    onNavigateBack: () -> Unit
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
            android.widget.Toast.LENGTH_SHORT
        ).show()
        uriHandler.openUri(pollUrl)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_us)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp),
        ) {

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.help_us_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (unrecordedBadges.isNotEmpty()) {
                Button(
                    onClick = {
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        scope.launch {
                            val result = PresetSubmissionHelper.submitUnrecordedBadges(unrecordedBadges)
                            isSubmitting = false
                            if (result.isSuccess) {
                                android.widget.Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.submit_badges_success,
                                        result.getOrElse { 0 }
                                    ),
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    R.string.submit_badges_failed,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                openQuestionnaireFallback()
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isSubmitting) {
                            stringResource(R.string.submit_badges_in_progress)
                        } else {
                            stringResource(R.string.submit_all_badges)
                        }
                    )
                }

                TextButton(
                    onClick = { openQuestionnaireFallback() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text(stringResource(R.string.copy_all_badges))
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (unrecordedBadges.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_unrecorded_badges),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(unrecordedBadges) { badge ->
                        UnrecordedBadgeItem(
                            badge = badge,
                            onCopySkCode = { skCode ->
                                clipboardManager.setText(AnnotatedString(skCode))
                            }
                        )
                        Divider()
                    }
                }
            }

        }
    }


}

@Composable
fun UnrecordedBadgeItem(
    badge: Badge,
    onCopySkCode: (String) -> Unit
) {
    val skCode = SkExtractor.getSkFromLinkOrNull(badge.link).orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = badge.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (badge.remark.isNotBlank()) {
            Text(
                text = badge.remark,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.sk_code, skCode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = { onCopySkCode(skCode) },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.copy))
            }
        }
    }
}
