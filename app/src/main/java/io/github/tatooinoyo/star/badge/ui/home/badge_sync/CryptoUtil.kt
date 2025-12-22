package io.github.tatooinoyo.star.badge.ui.home.badge_sync


import android.util.Base64
import org.bouncycastle.jcajce.spec.XDHParameterSpec
import org.bouncycastle.jce.provider.BouncyCastleProvider
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
    fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    fun d64(s: String): ByteArray = Base64.decode(s, Base64.NO_WRAP)
    private val bcProvider = BouncyCastleProvider()

    init {
        if (Security.getProvider("BC") == null) {
            Security.removeProvider("BC")
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateX25519KeyPair(): KeyPair {
        // 使用 "XDH" 算法名称，并配合 X25519 的参数规范
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
        salt: ByteArray = "fixed-salt".toByteArray()
    ): ByteArray {
        val spec = PBEKeySpec(code6.toCharArray(), salt, 10000, 256)
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


    fun aesGcmEncrypt(key: ByteArray, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val gcmSpec = GCMParameterSpec(128, iv)
        val sk = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, sk, gcmSpec)
        val ct = cipher.doFinal(plain)
        return iv + ct
    }


    fun aesGcmDecrypt(key: ByteArray, ivAndCipher: ByteArray): ByteArray {
        val iv = ivAndCipher.copyOfRange(0, 12)
        val cipherBytes = ivAndCipher.copyOfRange(12, ivAndCipher.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        val sk = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, sk, gcmSpec)
        return cipher.doFinal(cipherBytes)
    }
}