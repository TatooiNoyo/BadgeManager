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
    /** 生成测试链接时使用的默认 base（解析只依赖 s 参数，host 可任意）。 */
    const val DEFAULT_BASE_URL = "https://sky.thatgamecompany.com/u"

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

    /**
     * 由 SK 码生成徽章链接（与 [getSkFromLink] 互逆），用于测试未收录徽章等场景。
     *
     * @param sk SK 编码，如 `SKY-TEST-UNRECORDED-001`
     * @param baseUrl 链接前缀，默认 [DEFAULT_BASE_URL]
     */
    fun buildLinkFromSk(sk: String, baseUrl: String = DEFAULT_BASE_URL): String {
        require(sk.isNotBlank()) { "SK must not be blank" }
        val payload = "sk=$sk"
        val encoded = Base64.encodeToString(
            payload.toByteArray(Charset.forName("UTF-8")),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val separator = if (baseUrl.contains("?")) "&" else "?"
        return "$baseUrl${separator}s=$encoded"
    }
}
