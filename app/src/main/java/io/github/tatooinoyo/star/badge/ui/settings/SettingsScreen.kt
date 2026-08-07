package io.github.tatooinoyo.star.badge.ui.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.MainActivity
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.ui.component.BadgeContentCard
import io.github.tatooinoyo.star.badge.ui.component.LabeledInputField
import io.github.tatooinoyo.star.badge.ui.component.PrimaryOrangeButton
import io.github.tatooinoyo.star.badge.ui.component.SecondaryScreenHeader
import io.github.tatooinoyo.star.badge.ui.theme.BorderDefault
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrange
import io.github.tatooinoyo.star.badge.ui.theme.BrandOrangeLight
import io.github.tatooinoyo.star.badge.ui.theme.PeachTheme
import io.github.tatooinoyo.star.badge.ui.theme.TextPrimary
import io.github.tatooinoyo.star.badge.ui.theme.TextSecondary
import io.github.tatooinoyo.star.badge.utils.LanguageManager
import io.github.tatooinoyo.star.badge.utils.LanguageUtils
import io.github.tatooinoyo.star.badge.utils.SkExtractor

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val languageManager = LanguageManager.getInstance(context)
    var selectedLanguage by remember { mutableStateOf(languageManager.getCurrentLanguage()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PeachTheme)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        SecondaryScreenHeader(
            title = stringResource(R.string.settings),
            onBack = onNavigateBack,
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            BadgeContentCard {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "🌐 ${stringResource(R.string.language_setting)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandOrange,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LanguageOption(
                        text = stringResource(R.string.follow_system),
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_AUTO,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_AUTO
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_AUTO)
                            restartApp(context)
                        },
                    )
                    LanguageOption(
                        text = "English",
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_ENGLISH,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_ENGLISH
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_ENGLISH)
                            restartApp(context)
                        },
                    )
                    LanguageOption(
                        text = stringResource(R.string.chinese_simplified),
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_CHINESE,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_CHINESE
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_CHINESE)
                            restartApp(context)
                        },
                    )
                    LanguageOption(
                        text = stringResource(R.string.chinese_traditional),
                        isSelected = selectedLanguage == LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL,
                        onClick = {
                            selectedLanguage = LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL
                            languageManager.setLanguage(LanguageUtils.LANGUAGE_CHINESE_TRADITIONAL)
                            restartApp(context)
                        },
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

    BadgeContentCard {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🔗 ${stringResource(R.string.sk_link_tool_title)}",
                style = MaterialTheme.typography.titleMedium,
                color = BrandOrange,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.sk_link_tool_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            LabeledInputField(
                label = "SK",
                value = skInput,
                placeholder = stringResource(R.string.sk_link_tool_hint),
                onValueChange = { skInput = it },
            )

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryOrangeButton(
                text = stringResource(R.string.sk_link_tool_generate),
                onClick = {
                    val sk = skInput.trim()
                    if (sk.isBlank()) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.sk_link_tool_empty_sk),
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@PrimaryOrangeButton
                    }
                    generatedLink = SkExtractor.buildLinkFromSk(sk)
                },
            )

            generatedLink?.let { link ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.sk_link_tool_result),
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = link,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandOrange,
                )
                TextButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(link))
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_copy_success),
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
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
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = BrandOrange),
            )
        }
        HorizontalDivider(color = BorderDefault)
    }
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
