package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import org.json.JSONObject
import java.net.Socket
import java.security.SecureRandom

object HandshakeManager {

    fun handleServerSide(socket: Socket, psk6: String): ByteArray? {
        val input = socket.getInputStream()
        val out = socket.getOutputStream()

        val clientLine = CryptoUtil.readLineFromStream(input) ?: return null
        val clientObj = JSONObject(clientLine)
        if (clientObj.optString("type") != "hello") return null

        val clientPub = CryptoUtil.d64(clientObj.getString("pub"))
        val clientNonce = CryptoUtil.d64(clientObj.getString("nonce"))
        val clientRole = clientObj.getString("role")
        val clientHmac = CryptoUtil.d64(clientObj.getString("hmac"))

        val pskKey = CryptoUtil.derivePskKeyFromCode(psk6, clientNonce)
        val expected =
            CryptoUtil.hmacSha256(pskKey, clientPub + clientNonce + clientRole.toByteArray())
        if (!expected.contentEquals(clientHmac)) return null

        val kp = CryptoUtil.generateX25519KeyPair()
        val localPub = kp.public.encoded
        val localPriv = kp.private
        val localNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val role = "server"
        val hmac = CryptoUtil.hmacSha256(pskKey, localPub + localNonce + role.toByteArray())

        val reply = JSONObject()
        reply.put("type", "hello")
        reply.put("pub", CryptoUtil.b64(localPub))
        reply.put("nonce", CryptoUtil.b64(localNonce))
        reply.put("role", role)
        reply.put("hmac", CryptoUtil.b64(hmac))
        out.write((reply.toString() + "\n").toByteArray())
        out.flush()

        val shared = CryptoUtil.computeX25519SharedSecret(localPriv, clientPub)
        val salt = clientNonce + localNonce
        return CryptoUtil.hkdfSha256(shared, salt, null, 32)
    }

    fun handleClientSide(socket: Socket, psk6: String): ByteArray? {
        val input = socket.getInputStream()
        val out = socket.getOutputStream()

        val kp = CryptoUtil.generateX25519KeyPair()
        val localPub = kp.public.encoded
        val localPriv = kp.private
        val localNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val role = "client"
        val pskKey = CryptoUtil.derivePskKeyFromCode(psk6, localNonce)
        val hmac = CryptoUtil.hmacSha256(pskKey, localPub + localNonce + role.toByteArray())

        val hello = JSONObject()
        hello.put("type", "hello")
        hello.put("pub", CryptoUtil.b64(localPub))
        hello.put("nonce", CryptoUtil.b64(localNonce))
        hello.put("role", role)
        hello.put("hmac", CryptoUtil.b64(hmac))
        out.write((hello.toString() + "\n").toByteArray())
        out.flush()

        val srvLine = CryptoUtil.readLineFromStream(input) ?: return null
        val srvObj = JSONObject(srvLine)
        if (srvObj.optString("type") != "hello") return null
        val srvPub = CryptoUtil.d64(srvObj.getString("pub"))
        val srvNonce = CryptoUtil.d64(srvObj.getString("nonce"))
        val srvRole = srvObj.getString("role")
        val srvHmac = CryptoUtil.d64(srvObj.getString("hmac"))

        val expected = CryptoUtil.hmacSha256(pskKey, srvPub + srvNonce + srvRole.toByteArray())
        if (!expected.contentEquals(srvHmac)) return null

        val shared = CryptoUtil.computeX25519SharedSecret(localPriv, srvPub)
        val salt = localNonce + srvNonce
        return CryptoUtil.hkdfSha256(shared, salt, null, 32)
    }
}
