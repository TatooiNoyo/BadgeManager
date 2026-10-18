package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import org.json.JSONObject
import io.github.tatooinoyo.star.badge.utils.BadgeImportValidator

object SyncProtocol {
    const val PROTOCOL_VERSION = 2
    const val DISCOVERY_KEYWORD = "DISCOVER_STAR_APP"
    // JSON string escaping can double the data bytes; reserve room for the envelope and GCM.
    const val MAX_FRAME_BYTES = 2 * BadgeImportValidator.MAX_JSON_BYTES + 1024
    const val MAX_HANDSHAKE_BYTES = 4096
    const val MAX_PENDING_HANDSHAKES = 4
    const val DISCOVERY_TIMEOUT_MS = 60_000
    const val TRANSFER_TIMEOUT_MS = 60_000
    const val CONNECT_TIMEOUT_MS = 5_000
    const val HANDSHAKE_TIMEOUT_MS = 10_000
    /** Wait for receiver to acknowledge payload arrival (before user confirms import). */
    const val ACK_TIMEOUT_MS = 8_000
    /** Wait for user to confirm/cancel import after payload was received. */
    const val CONFIRM_TIMEOUT_MS = 120_000
    const val MAX_HANDSHAKE_FAILURES = 3
    const val PBKDF2_ITERATIONS = 100_000

    private val DISCOVERY_SALT = "discovery-salt-v2".toByteArray()

    /** Pre-connection fingerprint key; handshake uses per-session clientNonce salt. */
    fun discoveryPskKey(code6: String): ByteArray =
        CryptoUtil.derivePskKeyFromCode(code6, DISCOVERY_SALT, PBKDF2_ITERATIONS)

    fun discoveryFingerprint(code6: String): String {
        val mac = CryptoUtil.hmacSha256(discoveryPskKey(code6), "discovery".toByteArray())
        return CryptoUtil.b64(mac.copyOfRange(0, 4))
    }

    fun buildDiscoveryMessage(tcpPort: Int, code6: String): String =
        "$DISCOVERY_KEYWORD:$tcpPort:${discoveryFingerprint(code6)}"

    data class DiscoveryPacket(val tcpPort: Int, val fingerprint: String)

    fun parseDiscoveryMessage(message: String): DiscoveryPacket? {
        if (!message.startsWith("$DISCOVERY_KEYWORD:")) return null
        val rest = message.removePrefix("$DISCOVERY_KEYWORD:")
        val parts = rest.split(':')
        if (parts.size != 2) return null
        val port = parts[0].toIntOrNull() ?: return null
        if (port !in 1..65535) return null
        val fp = parts[1].trim()
        if (!fp.matches(Regex("[A-Za-z0-9+/]{6}=="))) return null
        return DiscoveryPacket(port, fp)
    }

    fun buildEnvelope(type: String, block: JSONObject.() -> Unit = {}): JSONObject =
        JSONObject().apply {
            put("v", PROTOCOL_VERSION)
            put("type", type)
            block()
        }

    fun requireVersion(obj: JSONObject) {
        val version = obj.optInt("v", 1)
        if (version != PROTOCOL_VERSION) {
            throw SyncProtocolException(SyncErrorCode.VERSION_INCOMPATIBLE)
        }
    }
}

class SyncProtocolException(val code: SyncErrorCode) : Exception(code.name)
