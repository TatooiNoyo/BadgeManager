package io.github.tatooinoyo.star.badge.ui.home.badge_sync

import org.bouncycastle.jcajce.spec.XDHParameterSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.InputStream
import java.io.IOException
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtil {
    fun b64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun d64(s: String): ByteArray = Base64.getDecoder().decode(s)
    private val bcProvider = BouncyCastleProvider()

    init {
        if (Security.getProvider("BC") == null) {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateX25519KeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("XDH", bcProvider)
        kpg.initialize(XDHParameterSpec("X25519"))
        return kpg.generateKeyPair()
    }

    fun computeX25519SharedSecret(privateKey: PrivateKey, peerPubBytes: ByteArray): ByteArray {
        val kf = KeyFactory.getInstance("XDH", bcProvider)
        val pubKeySpec = X509EncodedKeySpec(peerPubBytes)
        val pubK = kf.generatePublic(pubKeySpec)
        val ka = KeyAgreement.getInstance("XDH", bcProvider)
        ka.init(privateKey)
        ka.doPhase(pubK, true)
        return ka.generateSecret()
    }

    fun derivePskKeyFromCode(
        code6: String,
        salt: ByteArray,
        iterations: Int = SyncProtocol.PBKDF2_ITERATIONS,
    ): ByteArray {
        val spec = PBEKeySpec(code6.toCharArray(), salt, iterations, 256)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return skf.generateSecret(spec).encoded
    }

    fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }

    fun hkdfSha256(ikm: ByteArray, salt: ByteArray?, info: ByteArray?, length: Int): ByteArray {
        val actualSalt = salt ?: ByteArray(32) { 0 }
        val prk = hmacSha256(actualSalt, ikm)
        var t = ByteArray(0)
        val okm = ByteArray(length)
        var offset = 0
        var counter = 1
        while (offset < length) {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(prk, "HmacSHA256"))
            mac.update(t)
            if (info != null) mac.update(info)
            mac.update(counter.toByte())
            t = mac.doFinal()
            val toCopy = minOf(t.size, length - offset)
            System.arraycopy(t, 0, okm, offset, toCopy)
            offset += toCopy
            counter++
        }
        return okm
    }

    fun aesGcmEncrypt(key: ByteArray, plain: ByteArray, aad: ByteArray? = null): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(128, iv)
        val sk = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, sk, gcmSpec)
        if (aad != null) {
            cipher.updateAAD(aad)
        }
        val ct = cipher.doFinal(plain)
        return iv + ct
    }

    fun aesGcmDecrypt(key: ByteArray, ivAndCipher: ByteArray, aad: ByteArray? = null): ByteArray {
        require(ivAndCipher.size >= 12 + 16) { "Truncated GCM payload" }
        val iv = ivAndCipher.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        val sk = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, sk, gcmSpec)
        if (aad != null) {
            cipher.updateAAD(aad)
        }
        return cipher.doFinal(ivAndCipher, 12, ivAndCipher.size - 12)
    }

    /** Read bytes until LF without BufferedReader read-ahead. */
    fun readLineFromStream(input: InputStream, socket: Socket? = null, deadlineNanos: Long = Long.MAX_VALUE): String? {
        val sb = StringBuilder()
        var bytesRead = 0
        while (true) {
            if (socket != null) {
                val remaining = deadlineNanos - System.nanoTime()
                if (remaining <= 0) throw SocketTimeoutException("Handshake deadline exceeded")
                socket.soTimeout = TimeUnit.NANOSECONDS.toMillis(remaining).coerceIn(1, SyncProtocol.HANDSHAKE_TIMEOUT_MS.toLong()).toInt()
            }
            val b = input.read()
            if (b == -1) return if (sb.isEmpty()) null else throw IOException("Truncated handshake")
            if (b == '\n'.code) return sb.toString()
            if (++bytesRead > SyncProtocol.MAX_HANDSHAKE_BYTES) throw IOException("Handshake is too large")
            if (b != '\r'.code) {
                sb.append(b.toChar())
            }
        }
    }
}
