package com.example.mymusic.data.network

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    // 动态改为直连官方网易云
    var baseUrl: String = "https://music.163.com/"

    private var cookieStore: String = ""
    private var musicAToken: String = ""  // 匿名 MUSIC_A
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("netease_prefs", Context.MODE_PRIVATE)
        cookieStore = prefs.getString("cookie", "") ?: ""
        musicAToken = prefs.getString("music_a", "") ?: ""
        val savedUrl = prefs.getString("base_url", null)
        if (!savedUrl.isNullOrEmpty() && savedUrl.contains("music.163.com")) {
            baseUrl = savedUrl
        } else if (!savedUrl.isNullOrEmpty()) {
            // 旧架构遗留的 PC 服务器地址（如 10.0.2.2:8899），清除并使用默认直连
            prefs.edit().remove("base_url").apply()
            println("DEBUG ApiClient.init: cleared legacy base_url=$savedUrl, using default")
        }
        println("DEBUG ApiClient.init: has MUSIC_U=${cookieStore.contains("MUSIC_U", ignoreCase = true)}")
        println("DEBUG ApiClient.init: has MUSIC_A stored=${musicAToken.isNotEmpty()}")
        // 若无登录 Cookie 也无缓存 MUSIC_A，后台抓取匿名 Token
        if (!cookieStore.contains("MUSIC_U", ignoreCase = true) && musicAToken.isEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                val token = AnonymousToken.fetch()
                if (token != null) {
                    musicAToken = token
                    prefs.edit().putString("music_a", token).apply()
                    println("DEBUG ApiClient: MUSIC_A fetched and cached")
                }
            }
        }
    }

    fun saveCookie(cookie: String) {
        cookieStore = cookie
        prefs.edit().putString("cookie", cookie).apply()
    }

    fun clearCookie() {
        cookieStore = ""
        prefs.edit().remove("cookie").apply()
    }

    fun saveBaseUrl(url: String) {
        baseUrl = url
        prefs.edit().putString("base_url", url).apply()
    }

    fun getCookie(): String = cookieStore

    /**
     * 获取当前有效的 Cookie 字符串（包含 MUSIC_U 或 匿名 MUSIC_A）
     * 供外部组件（如 SuiXinChangApi）使用，确保能拿到登录态或匿名 Token
     */
    fun getEffectiveCookie(): String {
        val hasMusicU = cookieStore.contains("MUSIC_U", ignoreCase = true)
        if (cookieStore.isNotEmpty()) {
            return if (hasMusicU || musicAToken.isEmpty()) {
                cookieStore
            } else {
                "$cookieStore; $musicAToken"
            }
        } else if (musicAToken.isNotEmpty()) {
            return musicAToken
        }
        return ""
    }

    enum class NetIdentity {
        PC, ANDROID, TABLET
    }

    var currentIdentity: NetIdentity = NetIdentity.PC

    private val cookieInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // 1. 确定目标身份
        // 优先查看是否有 Header 指定（用于单次覆盖），否则看全局状态，eapi 请求强制用安卓
        val headerIdentity = originalRequest.header("X-Identity")
        val isEapi = originalRequest.url.toString().contains("/eapi/")
        val targetIdentity = when {
            headerIdentity == "TABLET" -> NetIdentity.TABLET
            headerIdentity == "ANDROID" -> NetIdentity.ANDROID
            headerIdentity == "PC" -> NetIdentity.PC
            isEapi -> NetIdentity.ANDROID // eapi 默认用安卓身份
            else -> currentIdentity
        }
        requestBuilder.removeHeader("X-Identity")

        // 2. 根据身份选择 User-Agent 和指纹
        val (ua, osFingerprint) = when (targetIdentity) {
            NetIdentity.PC -> {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0" to
                "os=pc; appver=3.0.18.203152; osver=Microsoft-Windows-10-Professional-build-19045-64bit"
            }
            NetIdentity.ANDROID -> {
                "NeteaseMusic/9.1.65.240927161425(9001065);Dalvik/2.1.0 (Linux; U; Android 14; 23013RK75C Build/UKQ1.230804.001)" to
                "os=android; appver=8.20.20.231215173437; osver=14; channel=xiaomi"
            }
            NetIdentity.TABLET -> {
                // 平板端模拟：使用 iPad 样式的 UA
                "Mozilla/5.0 (iPad; CPU OS 16_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148 NeteaseMusic/9.0.90" to
                "os=iPad OS; appver=9.0.90; osver=16.2; channel=distribution"
            }
        }

        requestBuilder.header("User-Agent", ua)
        if (targetIdentity == NetIdentity.PC) {
            requestBuilder.header("Referer", "https://music.163.com")
        }

        // 3. 注入 Cookie
        val hasMusicU = cookieStore.contains("MUSIC_U", ignoreCase = true)
        var finalCookie = ""
        
        // 基础登录态/匿名态
        if (cookieStore.isNotEmpty()) {
            finalCookie = if (hasMusicU || musicAToken.isEmpty()) {
                cookieStore
            } else {
                "$cookieStore; $musicAToken"
            }
        } else if (musicAToken.isNotEmpty()) {
            finalCookie = musicAToken
        }

        // 注入目标环境指纹
        finalCookie = if (finalCookie.isNotEmpty()) {
            "$finalCookie; $osFingerprint"
        } else {
            osFingerprint
        }

        requestBuilder.header("Cookie", finalCookie)
        
        val response = chain.proceed(requestBuilder.build())

        // 4. 保存 Cookie
        val setCookieHeaders = response.headers("Set-Cookie")
        if (setCookieHeaders.isNotEmpty()) {
            val allCookies = setCookieHeaders.joinToString("; ") { it.split(";").first().trim() }
            if (allCookies.contains("MUSIC_U", ignoreCase = true)) {
                saveCookie(allCookies)
                println("DEBUG Cookie: Saved new MUSIC_U cookie (Identity: $targetIdentity)")
            }
        }
        response
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun buildOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(cookieInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val service: NeteaseApiService by lazy { buildService() }

    private val lenientGson = GsonBuilder().setLenient().create()

    private fun buildService(): NeteaseApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildOkHttpClient())
            .addConverterFactory(LenientGsonConverterFactory.create(lenientGson))
            .build()
            .create(NeteaseApiService::class.java)
    }

    // 动态重建（修改 baseUrl 后调用）
    private var _service: NeteaseApiService? = null
    val dynamicService: NeteaseApiService
        get() = _service ?: buildService().also { _service = it }

    fun rebuildService() {
        _service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(buildOkHttpClient())
            .addConverterFactory(LenientGsonConverterFactory.create(lenientGson))
            .build()
            .create(NeteaseApiService::class.java)
    }
}
