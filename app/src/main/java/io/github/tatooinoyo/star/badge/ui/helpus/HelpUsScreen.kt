package io.github.tatooinoyo.star.badge.ui.helpus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.data.PresetBadges
import io.github.tatooinoyo.star.badge.utils.SkExtractor

@Composable
fun HelpUsScreen(
    badges: List<Badge>,
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.help_us),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

            }

            Spacer(modifier = Modifier.height(16.dp))

            // 未录入徽章列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val unrecordedBadges = badges.filter { badge ->
                    if (badge.link.isBlank()) return@filter false
                    val skCode = SkExtractor.getSkFromLink(badge.link)
                    if (skCode.isBlank()) return@filter false
                    // 检查 SK 码是否在预设列表中
                    PresetBadges.getTitle(context, skCode).isBlank()
                }

                if (unrecordedBadges.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无未录入徽章",
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
    val context = LocalContext.current
    val skCode = SkExtractor.getSkFromLink(badge.link)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 徽章标题
        Text(
            text = badge.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 徽章备注
        if (badge.remark.isNotBlank()) {
            Text(
                text = badge.remark,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // SK 码
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
                Text("复制")
            }
        }
    }
}