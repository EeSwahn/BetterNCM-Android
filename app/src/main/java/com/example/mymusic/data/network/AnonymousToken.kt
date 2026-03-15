package com.example.mymusic.data.network

import android.util.Base64
import com.example.mymusic.util.NeteaseCrypto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * 获取网易云匿名 Token（MUSIC_A）
 *
 * 完整还原 NeteaseCloudMusicApi 的 register_anonimous.js 逻辑：
 *  1. 生成 52 位随机十六进制 deviceId
 *  2. 对 deviceId 做 XOR + MD5 + Base64 编码 -> cloudmusic_dll_encode_id
 *  3. 将 "deviceId + ' ' + encodedId" 再 Base64 -> username
 *  4. 用 weapi 加密，POST 到 https://music.163.com/weapi/register/anonimous
 *  5. 从响应 Set-Cookie 头取出 MUSIC_A
 */
object AnonymousToken {

    private const val ID_XOR_KEY = "3go8\$&8*3*3h0k(2)2"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** 生成 52 位大写十六进制 deviceId */
    private fun generateDeviceId(): String {
        val hex = "0123456789ABCDEF"
        return (1..52).map { hex.random() }.joinToString("")
    }

    /** NeteaseCloudMusicApi 的 cloudmusic_dll_encode_id */
    private fun cloudmusicDllEncodeId(id: String): String {
        val xored = StringBuilder()
        for (i in id.indices) {
            val c = id[i].code xor ID_XOR_KEY[i % ID_XOR_KEY.length].code
            xored.append(c.toChar())
        }
        val md5Bytes = MessageDigest.getInstance("MD5").digest(xored.toString().toByteArray())
        return Base64.encodeToString(md5Bytes, Base64.NO_WRAP)
    }

    /** 完整拉取一次匿名 token，返回形如 "MUSIC_A=xxxxx" 的 Cookie 字符串，失败返回 null */
    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        try {
            val deviceId = generateDeviceId()
            val encodedId = cloudmusicDllEncodeId(deviceId)
            val username = Base64.encodeToString(
                "$deviceId $encodedId".toByteArray(),
                Base64.NO_WRAP
            )

            val data = mapOf("username" to username)
            val enc = NeteaseCrypto.encryptWeapi(Gson().toJson(data))

            val body = FormBody.Builder()
                .add("params", enc["params"]!!)
                .add("encSecKey", enc["encSecKey"]!!)
                .build()

            val request = Request.Builder()
                .url("https://music.163.com/weapi/register/anonimous")
                .post(body)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Safari/537.36 Chrome/91.0.4472.164")
                .header("Referer", "https://music.163.com")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = httpClient.newCall(request).execute()
            val setCookies = response.headers("Set-Cookie")

            // 提取所有 key=value 对（含 MUSIC_A、__csrf 等）
            val allKV = setCookies
                .mapNotNull { it.split(";").firstOrNull()?.trim() }
                .filter { it.isNotEmpty() }

            val musicA = allKV
                .firstOrNull { it.startsWith("MUSIC_A=") }
                ?.substringAfter("MUSIC_A=")

            response.close()

            if (!musicA.isNullOrBlank()) {
                println("DEBUG AnonymousToken: got MUSIC_A=${musicA.take(20)}...")
                // 把所有匿名 cookie（含 __csrf）都合并进 ApiClient，供后续 weapi 使用
                val cookieStr = allKV.joinToString("; ")
                ApiClient.saveCookie(cookieStr)
                println("DEBUG AnonymousToken: saved all anon cookies: $cookieStr")
                "MUSIC_A=$musicA"
            } else {
                println("DEBUG AnonymousToken: no MUSIC_A in Set-Cookie, headers=$setCookies")
                null
            }
        } catch (e: Exception) {
            println("DEBUG AnonymousToken: fetch failed: ${e.message}")
            null
        }
    }
}
