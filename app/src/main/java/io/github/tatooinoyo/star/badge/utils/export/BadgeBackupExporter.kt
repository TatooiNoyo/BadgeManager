package io.github.tatooinoyo.star.badge.utils.export

import com.google.gson.GsonBuilder
import io.github.tatooinoyo.star.badge.data.Badge
import java.io.IOException
import java.io.OutputStream

object BadgeBackupExporter {
    fun write(badges: List<Badge>, openOutputStream: () -> OutputStream?) {
        val json = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
            .toJson(badges)
        val output = openOutputStream() ?: throw IOException("Cannot open backup output")
        output.use {
            it.write(json.toByteArray(Charsets.UTF_8))
            it.flush()
        }
    }
}
