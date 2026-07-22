package io.github.tatooinoyo.star.badge.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.BuildConfig
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.utils.update.UpdateCheckResult
import io.github.tatooinoyo.star.badge.utils.update.UpdateSource

@Composable
fun UpdateCheckDialog(
    result: UpdateCheckResult?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onDismissVersion: (String) -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.check_update)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.current_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodyLarge,
                )
                when {
                    loading || result == null -> {
                        Text(
                            stringResource(R.string.update_checking),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    result is UpdateCheckResult.Available -> {
                        Text(
                            stringResource(R.string.update_available, result.info.versionName),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(sourceLabelRes(result.info.source)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (result.fellBackFromWorker) {
                            Text(
                                stringResource(R.string.update_fallback_github),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (result.info.releaseNotes.isNotBlank()) {
                            Text(
                                result.info.releaseNotes.take(500),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    result is UpdateCheckResult.UpToDate -> {
                        Text(
                            stringResource(R.string.update_up_to_date),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            stringResource(R.string.latest_version_value, result.latestVersion),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(sourceLabelRes(result.source)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (result.fellBackFromWorker) {
                            Text(
                                stringResource(R.string.update_fallback_github),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    result is UpdateCheckResult.Failed -> {
                        Text(
                            stringResource(R.string.update_check_failed),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            result.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (result) {
                is UpdateCheckResult.Available -> {
                    Button(
                        onClick = {
                            uriHandler.openUri(result.info.downloadUrl)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.update_download))
                    }
                }
                else -> {
                    if (!loading) {
                        Button(onClick = onDismiss) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        },
        dismissButton = {
            when {
                loading -> Unit
                result is UpdateCheckResult.Available -> {
                    TextButton(
                        onClick = {
                            onDismissVersion(result.info.versionName)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.update_later))
                    }
                }
                result is UpdateCheckResult.Failed -> {
                    val fallback = stringResource(R.string.download_url)
                    TextButton(
                        onClick = {
                            uriHandler.openUri(fallback)
                            onDismiss()
                        }
                    ) {
                        Text(stringResource(R.string.update_open_releases))
                    }
                }
                else -> Unit
            }
        },
    )
}

private fun sourceLabelRes(source: UpdateSource): Int = when (source) {
    UpdateSource.GITHUB -> R.string.update_source_github
    UpdateSource.PGYER -> R.string.update_source_pgyer
}
