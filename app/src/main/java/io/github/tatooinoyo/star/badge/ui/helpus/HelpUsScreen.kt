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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
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
import androidx.compose.runtime.remember
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
import io.github.tatooinoyo.star.badge.utils.BadgeFormatterUtils
import io.github.tatooinoyo.star.badge.utils.SkExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpUsScreen(
    badges: List<Badge>,
    onNavigateBack: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val unrecordedBadges = remember(badges) {
        badges.filter { badge ->
            if (badge.link.isBlank()) return@filter false
            val skCode = SkExtractor.getSkFromLink(badge.link)
            if (skCode.isBlank()) return@filter false
            PresetBadges.getTitle(context, skCode).isBlank()
        }
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

            // 添加说明文字
            Text(
                text = stringResource(R.string.help_us_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (unrecordedBadges.isNotEmpty()) {
                Button(
                    onClick = {
                        val badgesInfo = BadgeFormatterUtils.formatUnrecordedBadges(context, unrecordedBadges)
                        clipboardManager.setText(AnnotatedString(badgesInfo))
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.copy_all_badges_success, unrecordedBadges.size),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.copy_all_badges))
                }
            }

            // 未录入徽章列表
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
                Text(stringResource(R.string.copy))
            }
        }
    }
}