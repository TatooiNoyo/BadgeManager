package io.github.tatooinoyo.star.badge.ui.home.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import io.github.tatooinoyo.star.badge.ui.component.BadgeAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R
import io.github.tatooinoyo.star.badge.ui.home.QiguangImportUiState

@Composable
fun QiguangImportDialog(
    state: QiguangImportUiState,
    onDismiss: () -> Unit,
    onUnlock: (String) -> Unit,
    onImport: () -> Unit,
) {
    var password by remember(state.filePath) { mutableStateOf("") }
    val canImport = !state.passwordRequired && state.format != null

    LaunchedEffect(state.passwordRequired) {
        if (!state.passwordRequired) password = ""
    }

    BadgeAlertDialog(
        onDismissRequest = { if (!state.isProcessing) onDismiss() },
        title = { Text(stringResource(R.string.qiguang_import_title)) },
        text = {
            Column {
                if (state.format == null) {
                    Text(
                        text = stringResource(R.string.qiguang_import_analyzing),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.qiguang_import_format, state.format.displayName),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                if (state.passwordRequired) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.qiguang_import_password_desc),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { if (it.length <= 1024) password = it },
                        label = { Text(stringResource(R.string.qiguang_import_password_label)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (canImport) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            R.string.qiguang_import_summary,
                            state.badgeCount,
                            state.skippedCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                if (state.encryptedShare) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.qiguang_import_share_warning),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (state.passwordRequired) onUnlock(password) else onImport()
                },
                enabled = !state.isProcessing &&
                    (if (state.passwordRequired) password.isNotEmpty() else canImport),
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        stringResource(
                            if (state.passwordRequired) R.string.qiguang_import_unlock
                            else R.string.qiguang_import_confirm,
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isProcessing) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
