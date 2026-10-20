package io.github.tatooinoyo.star.badge

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import io.github.tatooinoyo.star.badge.data.Badge
import io.github.tatooinoyo.star.badge.ui.home.HomeViewModel
import io.github.tatooinoyo.star.badge.utils.export.BadgeShareExporter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.*
import org.junit.Test

class ShareIntentRecreationTest {
    @Test fun sharedFileIsNotReprocessedOnRecreation() = verifyRecreation(Intent.ACTION_SEND)

    @Test fun openedFileIsNotReprocessedOnRecreation() = verifyRecreation(Intent.ACTION_VIEW)

    private fun verifyRecreation(action: String) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = BadgeShareExporter.writeEncryptedFile(
            context, listOf(Badge(title = "Shared badge", remark = "", orderIndex = 1)), "123456",
        )
        val intent = shareIntent(context, action, file.uri)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            lateinit var model: HomeViewModel
            scenario.onActivity { model = ViewModelProvider(it)[HomeViewModel::class.java] }
            val pending = withTimeout(10_000) { model.uiState.first { it.pendingImportUri != null } }.pendingImportUri

            // 未完成的导入保留原缓存 URI，不能再次复制文件并重置导入对话框。
            scenario.recreate()
            assertNull(withTimeoutOrNull(1_000) {
                model.uiState.first { it.pendingImportUri != pending }
            })

            scenario.onActivity { model.dismissShareImport() }
            scenario.recreate()
            assertNull(withTimeoutOrNull(1_000) {
                model.uiState.first { it.pendingImportUri != null }
            })

            // 用户主动再次打开相同文件时，仍应作为新的导入处理。
            scenario.onActivity { it.startActivity(shareIntent(context, action, file.uri)) }
            val reopened = withTimeout(10_000) { model.uiState.first { it.pendingImportUri != null } }
            assertNotEquals(pending, reopened.pendingImportUri)
        }
    }

    private fun shareIntent(context: Context, action: String, uri: Uri): Intent =
        Intent(context, MainActivity::class.java).apply {
            this.action = action
            type = "application/x-badgeenc"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            if (action == Intent.ACTION_SEND) putExtra(Intent.EXTRA_STREAM, uri) else data = uri
        }
}
