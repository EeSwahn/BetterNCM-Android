package com.example.mymusic.data.network

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SuiXinChangApi {
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private val client = OkHttpClient()
    private val gson = Gson()

    private fun md5(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digested = md.digest(text.toByteArray())
        return digested.joinToString("") { "%02x".format(it) }
    }

    private fun encryptEapi(url: String, dataJson: String): String {
        val digest = md5("nobody${url}use${dataJson}md5forencrypt")
        val plain = "${url}-36cd479b6b5-${dataJson}-36cd479b6b5-${digest}"
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(EAPI_KEY.toByteArray(), "AES"))
        return cipher.doFinal(plain.toByteArray()).joinToString("") { "%02X".format(it) }
    }

    suspend fun getAccompanyUrl(songId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val data = mapOf(
                "ids" to "[\"${songId}_0\"]",
                "level" to "exhigh",
                "encodeType" to "aac",
                "trialMode" to "3",
                "immerseType" to "c51",
                "degradeImmerseType" to "c51",
                "expParams" to "{\"hy-zyyinzhi\":\"t1\"}",
                "cliUserId" to "0",
                "cliVipTypes" to "[]",
                "trialModes" to "{\"$songId\":3}",
                "supportDolby" to "false",
                "volumeBalance" to "1",
                "djVolumeBalance" to "1",
                "accompany" to "true"
            )
            val json = gson.toJson(data)
            val params = encryptEapi("/api/song/enhance/player/url/v1", json)
            val body = FormBody.Builder().add("params", params).build()
            // 动态获取当前登录态 Cookie，并注入移动端特征
            val effectiveCookie = ApiClient.getEffectiveCookie()
            val androidCookies = "os=android; appver=8.20.20.231215173437; osver=14; channel=netease"
            val finalCookie = if (effectiveCookie.isNotEmpty()) "$effectiveCookie; $androidCookies" else androidCookies

            val request = Request.Builder()
                .url("https://interface3.music.163.com/eapi/song/enhance/player/url/v1")
                .post(body)
                .addHeader("User-Agent", "NeteaseMusic/9.1.65.240927161425(9001065);Dalvik/2.1.0 (Linux; U; Android 14; 23013RK75C Build/UKQ1.230804.001)")
                .addHeader("Cookie", finalCookie)
                .build()

            val response = client.newCall(request).execute()
            val respBody = response.body?.string() ?: return@withContext null
            val result = gson.fromJson(respBody, ApiResult::class.java)
            
            // Note: Netease might return the URL if accompany is available.
            return@withContext result.data?.firstOrNull()?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private data class ApiResult(val data: List<ApiData>?)
    private data class ApiData(val url: String?, val accompany: Boolean?)
}
