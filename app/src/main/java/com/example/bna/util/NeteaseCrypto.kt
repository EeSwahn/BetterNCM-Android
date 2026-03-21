package com.example.bna.util

import android.util.Base64
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object NeteaseCrypto {
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private const val WEAPI_IV = "0102030405060708"
    private const val WEAPI_PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val WEAPI_PUB_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDgtQn2JZ34ZC28NWYpAUd98iZ37BUrX/aKzmFbt7clFSs6sXqHauqKWqdtLkF2KexO40H1YTX8z2lSgBBOAxLsvaklV8k4cBFK9snQXE9/DDaFt6Rr7iVZMldczhC0JNgTz+SHXT6CBHuX3e9SdB1Ua44oncaTWz7OBGLbCiK45wIDAQAB"

    private fun md5(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ========== EAPI ==========
    fun encryptEapi(url: String, dataJson: String): String {
        val digest = md5("nobody${url}use${dataJson}md5forencrypt")
        val plain = "${url}-36cd479b6b5-${dataJson}-36cd479b6b5-${digest}"
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(), "AES"))
        return cipher.doFinal(plain.toByteArray()).joinToString("") { "%02X".format(it) }
    }

    fun decryptEapiResponse(hexCipher: String): String {
        val bytes = hexStringToByteArray(hexCipher)
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(), "AES"))
        return String(cipher.doFinal(bytes))
    }

    // ========== WEAPI ==========
    fun encryptWeapi(text: String): Map<String, String> {
        val secretKey = getRandomString(16)
        val base64_1 = aesEncryptCbc(text, WEAPI_PRESET_KEY, WEAPI_IV)
        val params = aesEncryptCbc(base64_1, secretKey, WEAPI_IV)
        val encSecKey = rsaEncryptNoPadding(secretKey.reversed().toByteArray())
        return mapOf(
            "params" to params,
            "encSecKey" to encSecKey
        )
    }

    private fun aesEncryptCbc(text: String, key: String, iv: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"), IvParameterSpec(iv.toByteArray()))
        return Base64.encodeToString(cipher.doFinal(text.toByteArray()), Base64.NO_WRAP)
    }

    private fun rsaEncryptNoPadding(text: ByteArray): String {
        val keySpec = X509EncodedKeySpec(Base64.decode(WEAPI_PUB_KEY, Base64.DEFAULT))
        val publicKey = KeyFactory.getInstance("RSA").generatePublic(keySpec)
        val cipher = Cipher.getInstance("RSA/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        
        val input = ByteArray(128)
        System.arraycopy(text, 0, input, 128 - text.size, text.size)
        return cipher.doFinal(input).joinToString("") { "%02x".format(it) }
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { allowedChars.random() }.joinToString("")
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
