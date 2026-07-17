package io.github.tatooinoyo.star.badge.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.MainActivity
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.utils.LanguageManager
import io.github.tatooinoyo.star.badge.utils.LanguageUtils
import io.github.tatooinoyo.star.badge.utils.SkExtractor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val languageManager = LanguageManager.getInstance(context)
    var selectedLanguage by remember { mutableStateOf(languageManager.getCurrentLanguage()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
                .verticalScroll(rememberScrollState())
        ) {
            // 语言设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.language_setting),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.padding(8.dp))

                    LanguageOption(
                        text = stringResource(R.string.follow_system),
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_AUTO,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_AUTO
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_AUTO)
                            restartApp(context)
                        }
                    )

                    LanguageOption(
                        text = "English",
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_ENGLISH,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_ENGLISH
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_ENGLISH)
                            restartApp(context)
                        }
                    )

                    LanguageOption(
                        text = "简体中文",
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_CHINESE,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_CHINESE
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_CHINESE)
                            restartApp(context)
                        }
                    )

                    LanguageOption(
                        text = "繁體中文",
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL)
                            restartApp(context)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SkLinkGeneratorCard()
        }
    }
}

@Composable
private fun SkLinkGeneratorCard() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var skInput by remember { mutableStateOf("SKY-TEST-UNRECORDED-001") }
    var generatedLink by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.sk_link_tool_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sk_link_tool_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = skInput,
                onValueChange = { skInput = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.sk_link_tool_hint)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    val sk = skInput.trim()
                    if (sk.isBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.sk_link_tool_empty_sk),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    generatedLink = SkExtractor.buildLinkFromSk(sk)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sk_link_tool_generate))
            }

            generatedLink?.let { link ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.sk_link_tool_result),
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(link))
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_copy_success),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text(stringResource(R.string.copy))
                }
            }
        }
    }
}

@Composable
fun LanguageOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    )
}

fun restartApp(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
    if (context is MainActivity) {
        context.finish()
    }
    Runtime.getRuntime().exit(0)
}

@Composable
@Preview
fun SettingsScreenPreview() {
    SettingsScreen(onNavigateBack = {})
}
