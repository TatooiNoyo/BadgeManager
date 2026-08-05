package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import org.json.JSONObject

object SyncProtocol {
    const val PROTOCOL_VERSION = 2
    const val DISCOVERY_KEYWORD = "DISCOVER_STAR_APP"
    const val MAX_FRAME_BYTES = 16 * 1024 * 1024
    const val CONNECT_TIMEOUT_MS = 5_000
    const val HANDSHAKE_TIMEOUT_MS = 10_000
    const val ACK_TIMEOUT_MS = 8_000
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
        val fp = parts[1].trim()
        if (fp.isEmpty()) return null
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
