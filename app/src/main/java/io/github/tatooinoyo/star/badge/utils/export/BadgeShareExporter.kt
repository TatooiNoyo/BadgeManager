package io.github.tatooinoyo.star.badge.utils.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import io.github.tatooinoyo.star.badge.data.Badge
import java.io.File

object BadgeShareExporter {

    private const val EXPORT_DIR = "shared_exports"
    private const val FILE_EXT = ".badgeenc"
    private const val MIME_FILE = "application/x-badgeenc"
    private const val MAX_CACHED_FILES = 5

    data class ShareExportResult(
        val uri: Uri,
        val mimeType: String,
        val code: String,
    )

    fun writeEncryptedFile(
        context: Context,
        badges: List<Badge>,
        code: String,
    ): ShareExportResult {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        cleanupOldFiles(dir)
        val timestamp = System.currentTimeMillis()
        val encrypted = BadgeShareCrypto.encrypt(badges, code)
        val fileName = "BadgeShare_${badges.size}_${timestamp}$FILE_EXT"
        val file = File(dir, fileName)
        file.writeBytes(encrypted)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return ShareExportResult(uri, MIME_FILE, code)
    }

    private fun cleanupOldFiles(dir: File) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(MAX_CACHED_FILES).forEach { it.delete() }
    }

    fun copyIncomingShareToCache(context: Context, sourceUri: Uri): Uri {
        val dir = File(context.cacheDir, "$EXPORT_DIR/incoming").apply { mkdirs() }
        val bytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Cannot read shared content")
        if (!BadgeShareCrypto.looksLikeEncryptedShare(bytes)) {
            throw BadgeShareError.InvalidFile
        }
        val file = File(dir, "import_${System.currentTimeMillis()}$FILE_EXT")
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
    }
}
