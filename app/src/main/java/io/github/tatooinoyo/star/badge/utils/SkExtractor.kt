package io.github.tatooinoyo.star.badge.utils

import android.util.Base64
import java.nio.charset.Charset

object SkExtractor {
    fun getSkFromLink(link: String): String {
        return try {
            val uri = android.net.Uri.parse(link)
            val sParam = uri.getQueryParameter("s")

            if (sParam.isNullOrBlank()) {
                ""
            } else {
                // Base64 解码
                val decodedBytes = Base64.decode(sParam, Base64.URL_SAFE) // Sky 的链接通常是 URL_SAFE
                val decodedString = String(decodedBytes, Charset.forName("UTF-8"))

                // 从解码后的字符串中提取 sk 参数
                val decodedUri = android.net.Uri.parse("dummy://host?$decodedString")
                decodedUri.getQueryParameter("sk") ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}