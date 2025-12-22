package io.github.tatooinoyo.star.badge.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.MainActivity
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.utils.LanguageManager
import io.github.tatooinoyo.star.badge.utils.LanguageUtils

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
                    
                    // 语言选择选项
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
            .clickable(onClick = onClick)  // 添加点击事件
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