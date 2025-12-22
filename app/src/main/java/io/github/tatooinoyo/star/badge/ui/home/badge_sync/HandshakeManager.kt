package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.security.SecureRandom


object HandshakeManager {


    // 服务器端处理：接收 client hello -> 验证 -> 回复 server hello -> 生成 session key
    fun handleServerSide(socket: Socket, psk6: String): ByteArray? {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val out = socket.getOutputStream()


        val clientLine = reader.readLine() ?: return null
        val clientObj = JSONObject(clientLine)
        if (clientObj.getString("type") != "hello") return null


        val clientPub = CryptoUtil.d64(clientObj.getString("pub"))
        val clientNonce = CryptoUtil.d64(clientObj.getString("nonce"))
        val clientRole = clientObj.getString("role")
        val clientHmac = CryptoUtil.d64(clientObj.getString("hmac"))


        val pskKey = CryptoUtil.derivePskKeyFromCode(psk6)
        val expected =
            CryptoUtil.hmacSha256(pskKey, clientPub + clientNonce + clientRole.toByteArray())
        if (!expected.contentEquals(clientHmac)) return null


        // 生成本地 ephemeral key
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


        // 计算共享 secret -> sessionKey
        val shared = CryptoUtil.computeX25519SharedSecret(localPriv, clientPub)
        val salt = clientNonce + localNonce // 双方约定顺序
        val sessionKey = CryptoUtil.hkdfSha256(shared, salt, null, 32)

        return sessionKey
    }


    fun handleClientSide(socket: Socket, psk6: String): ByteArray? {
        val out = socket.getOutputStream()
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))


        // 发出 client hello
        val kp = CryptoUtil.generateX25519KeyPair()
        val localPub = kp.public.encoded
        val localPriv = kp.private
        val localNonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val role = "client"
        val pskKey = CryptoUtil.derivePskKeyFromCode(psk6)
        val hmac = CryptoUtil.hmacSha256(pskKey, localPub + localNonce + role.toByteArray())


        val hello = JSONObject()
        hello.put("type", "hello")
        hello.put("pub", CryptoUtil.b64(localPub))
        hello.put("nonce", CryptoUtil.b64(localNonce))
        hello.put("role", role)
        hello.put("hmac", CryptoUtil.b64(hmac))
        out.write((hello.toString() + "\n").toByteArray())
        out.flush()


        // 读 server hello
        val srvLine = reader.readLine() ?: return null
        val srvObj = JSONObject(srvLine)
        if (srvObj.getString("type") != "hello") return null
        val srvPub = CryptoUtil.d64(srvObj.getString("pub"))
        val srvNonce = CryptoUtil.d64(srvObj.getString("nonce"))
        val srvRole = srvObj.getString("role")
        val srvHmac = CryptoUtil.d64(srvObj.getString("hmac"))


        val expected = CryptoUtil.hmacSha256(pskKey, srvPub + srvNonce + srvRole.toByteArray())
        if (!expected.contentEquals(srvHmac)) return null


        val shared = CryptoUtil.computeX25519SharedSecret(localPriv, srvPub)
        val salt = localNonce + srvNonce
        val sessionKey = CryptoUtil.hkdfSha256(shared, salt, null, 32)

        return sessionKey
    }
}