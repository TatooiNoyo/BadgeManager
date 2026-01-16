package io.github.tatooinoyo.star.badge.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import io.github.tatooinoyo.star.badge.BuildConfig
import io.github.tatooinoyo.star.badge.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val projectUrl = "https://github.com/tatooinoyo/BadgeManager"
    val issuesUrl = "$projectUrl/issues"
    val pollUrl = "https://f.wps.cn/g/RQq78MAA"
    val contactMail = "tatooi.noyo@outlook.com"

    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateImageUrl = "https://badgen.net/github/release/tatooinoyo/BadgeManager"
    val downloadUrl = "https://www.pgyer.com/badgemanager"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.help_title)) },
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
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "V${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Text(stringResource(R.string.about_badge_manager_desc))

            HorizontalDivider()

            // 使用帮助部分
            Text(
                text = stringResource(R.string.usage_help_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.usage_help_floating_menu),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.usage_help_landscape_only),
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider()

            // 仓库链接
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(projectUrl) }, // 点击跳转
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.help_repo),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                )
            }

            // 建议链接
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(issuesUrl) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.help_feedback),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }

            // 徽章信息采集
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { uriHandler.openUri(pollUrl) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.help_poll),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                )
            }
            Text(stringResource(R.string.badge_not_found_help))


            // 联系作者
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.contact_me),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
            SelectionContainer() { Text(contactMail) }

            Button(
                onClick = { showUpdateDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("检查更新")
            }
        }

        if (showUpdateDialog) {
            Dialog(onDismissRequest = { showUpdateDialog = false }) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge, // 圆角
                    tonalElevation = 6.dp, // Material 3 的色调海拔（提供阴影和背景色深度）
                    color = MaterialTheme.colorScheme.surface, // 背景色
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "当前版本: V${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Row(modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("最新版本: ", style = MaterialTheme.typography.headlineSmall)

                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(updateImageUrl)
                                    .decoderFactory(SvgDecoder.Factory()) // 必须指定 SVG 解码器
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "最新版本",
                                modifier = Modifier
                                    .height(30.dp)
                                    .clickable { uriHandler.openUri(downloadUrl) }
                            )
                        }
                        Text(
                            text = "点击上方图标前往下载页",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = { showUpdateDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("确定")
                        }
                    }
                }

            }
        }
    }
}