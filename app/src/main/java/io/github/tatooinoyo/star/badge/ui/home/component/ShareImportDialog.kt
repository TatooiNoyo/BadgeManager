package io.github.tatooinoyo.star.badge.ui.home.component

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.tatooinoyo.star.badge.R

@Composable
fun ShareImportDialog(
    uri: Uri,
    onDismiss: () -> Unit,
    onImportSharedBadges: (Context, Uri, String, (Int) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var importCode by rememberSaveable(uri) { mutableStateOf("") }
    var isImporting by rememberSaveable(uri) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = {
            if (!isImporting) onDismiss()
        },
        title = { Text(stringResource(R.string.share_import_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.share_import_external_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = importCode,
                    onValueChange = { value ->
                        if (value.length <= 6 && value.all { it.isDigit() }) {
                            importCode = value
                        }
                    },
                    label = { Text(stringResource(R.string.enter_6_digit_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isImporting = true
                    onImportSharedBadges(context, uri, importCode) { count ->
                        isImporting = false
                        if (count > 0) {
                            onDismiss()
                            Toast.makeText(
                                context,
                                context.getString(R.string.share_import_success, count),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                },
                enabled = importCode.length == 6 && !isImporting,
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
