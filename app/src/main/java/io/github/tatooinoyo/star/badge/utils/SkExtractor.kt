package io.github.tatooinoyo.star.badge.utils

import android.util.Base64
import java.nio.charset.Charset

class SkExtractException(
    val reason: Reason,
    cause: Throwable? = null
) : Exception(reason.name, cause) {
    enum class Reason {
        MISSING_S_PARAM,
        MISSING_SK_PARAM,
        DECODE_FAILED
    }
}

object SkExtractor {
    /**
     * 从链接中提取 SK 码。解析失败时抛出 [SkExtractException]，由调用方捕获并展示错误。
     */
    @Throws(SkExtractException::class)
    fun getSkFromLink(link: String): String {
        try {
            val uri = android.net.Uri.parse(link)
            val sParam = uri.getQueryParameter("s")

            if (sParam.isNullOrBlank()) {
                throw SkExtractException(SkExtractException.Reason.MISSING_S_PARAM)
            }

            val decodedBytes = Base64.decode(sParam, Base64.URL_SAFE)
            val decodedString = String(decodedBytes, Charset.forName("UTF-8"))
            val decodedUri = android.net.Uri.parse("dummy://host?$decodedString")
            return decodedUri.getQueryParameter("sk")
                ?: throw SkExtractException(SkExtractException.Reason.MISSING_SK_PARAM)
        } catch (e: SkExtractException) {
            throw e
        } catch (e: Exception) {
            throw SkExtractException(SkExtractException.Reason.DECODE_FAILED, e)
        }
    }

    /** 解析失败时返回 null，适用于不需要展示错误的场景。 */
    fun getSkFromLinkOrNull(link: String): String? {
        return try {
            getSkFromLink(link)
        } catch (_: SkExtractException) {
            null
        }
    }
}
