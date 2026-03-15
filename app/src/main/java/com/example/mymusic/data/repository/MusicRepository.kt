package com.example.mymusic.data.repository

import com.example.mymusic.data.model.*
import com.example.mymusic.data.network.ApiClient
import com.example.mymusic.util.NeteaseCrypto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

class MusicRepository {

    private val api get() = ApiClient.dynamicService
    private val gson = Gson()

    private fun weapi(data: Map<String, Any>): Map<String, String> {
        // 与 NeteaseCloudMusicApi request.js 对齐：加密 body 里需要包含 csrf_token
        // 从已存 Cookie 中提取 __csrf；未登录时为空字符串
        val csrf = ApiClient.getCookie()
            .split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("__csrf=") }
            ?.substringAfter("__csrf=")
            ?: ""
        val dataWithCsrf = data + mapOf("csrf_token" to csrf)
        val jsonStr = gson.toJson(dataWithCsrf)
        return NeteaseCrypto.encryptWeapi(jsonStr)
    }

    private fun md5(text: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ========== 登录 ==========

    /** 发送手机验证码 */
    suspend fun sendCaptcha(phone: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf("ctcode" to "86", "cellphone" to phone)
                val enc = weapi(data)
                val response = api.sendCaptcha(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) Result.Success(Unit)
                    else Result.Error(body?.msg ?: body?.message ?: "发送失败，请稍后重试", body?.code ?: -1)
                } else {
                    Result.Error("网络错误: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    /** 验证码登录（无需密码） */
    suspend fun loginByCaptcha(phone: String, captcha: String): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "phone" to phone,
                    "captcha" to captcha,
                    "countrycode" to "86",
                    "rememberLogin" to "true"
                )
                val enc = weapi(data)
                val response = api.loginByPhone(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        val setCookie = response.headers().values("Set-Cookie")
                        if (setCookie.isNotEmpty()) {
                            val cleanCookie = setCookie
                                .mapNotNull { it.split(";").firstOrNull()?.trim() }
                                .joinToString("; ")
                            ApiClient.saveCookie(cleanCookie)
                        }
                        Result.Success(body)
                    } else {
                        Result.Error(body?.msg ?: body?.message ?: "登录失败", body?.code ?: -1)
                    }
                } else {
                    Result.Error("网络错误: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    suspend fun loginByPhone(phone: String, password: String, identity: String? = null): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                // 标准 MD5 密码（网易云 cellphone 登录要求明文 MD5）
                val md5pwd = md5(password)
                val data = mapOf("phone" to phone, "password" to md5pwd, "countrycode" to "86", "rememberLogin" to "true")
                val enc = weapi(data)
                
                // 这里暂时没有在 Service 中为 loginByPhone 加 Header，但可以通过 ApiClient.currentIdentity 全局控制
                val response = api.loginByPhone(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.code == 200) {
                        val setCookie = response.headers().values("Set-Cookie")
                        if (setCookie.isNotEmpty()) {
                            ApiClient.saveCookie(setCookie.joinToString("; "))
                        }
                        Result.Success(body)
                    } else {
                        Result.Error(body?.msg ?: body?.message ?: "登录失败", body?.code ?: -1)
                    }
                } else {
                    Result.Error("网络错误: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    suspend fun getLoginStatus(): Result<LoginStatusResponse> =
        withContext(Dispatchers.IO) {
            try {
                val enc = weapi(emptyMap())
                val response = api.getLoginStatus(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) Result.Success(body)
                    else Result.Error("获取登录状态失败")
                } else {
                    Result.Error("网络错误: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val enc = weapi(emptyMap())
                api.logout(enc["params"]!!, enc["encSecKey"]!!)
                ApiClient.clearCookie()
                Result.Success(Unit)
            } catch (e: Exception) {
                ApiClient.clearCookie()
                Result.Success(Unit)
            }
        }

    // QR 登录
    // type: 1 为 PC 扫码, 3 为 APP 扫码, 2 为平板版扫码
    suspend fun getQrKey(type: Int = 3, identity: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val enc = weapi(mapOf("type" to type))
                val response = api.getQrKey(enc["params"]!!, enc["encSecKey"]!!, identity)
                val body = response.body()
                val key = body?.data?.unikey ?: body?.unikey
                if (response.isSuccessful && key != null) {
                    Result.Success(key)
                } else {
                    Result.Error("获取Key失败")
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    suspend fun getQrKeyTablet(): Result<String> = getQrKey(type = 2, identity = "TABLET")

    // 本地直接返回 qrurl，UI 侧使用 ZXing 本地生成 Bitmap
    suspend fun createQrCode(key: String, type: Int = 3): Result<QrCreateData> =
        withContext(Dispatchers.IO) {
            Result.Success(QrCreateData(qrurl = "https://music.163.com/login?codekey=$key", qrimg = null))
        }

    suspend fun checkQrStatus(
        key: String, 
        type: Int = 3, 
        identity: String? = null,
        useMobileEndpoint: Boolean = false
    ): Result<QrCheckResponse> =
        withContext(Dispatchers.IO) {
            try {
                val enc = weapi(mapOf("type" to type, "key" to key))
                val response = if (useMobileEndpoint) {
                    api.checkQrStatusMobile(enc["params"]!!, enc["encSecKey"]!!, identity)
                } else {
                    api.checkQrStatus(enc["params"]!!, enc["encSecKey"]!!, identity)
                }
                
                val body = response.body()
                println("DEBUG Repository: checkQrStatus code=${body?.code}, hasCookie=${!body?.cookie.isNullOrEmpty()}")
                
                if (response.isSuccessful && body != null) {
                    if (body.code == 803 && !body.cookie.isNullOrEmpty()) {
                        // 提取真正的 MUSIC_U 等核心部分，过滤无用字段
                        val rawCookie = body.cookie.replace("HTTPOnly", "")
                        ApiClient.saveCookie(rawCookie)
                        println("DEBUG Repository: Saved cookie from 803 body")
                    }
                    Result.Success(body)
                } else {
                    Result.Error("检查二维码状态失败")
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    suspend fun checkQrStatusTablet(key: String): Result<QrCheckResponse> = 
        checkQrStatus(key, type = 2, identity = "TABLET", useMobileEndpoint = true)

    // ========== 搜索 ==========

    suspend fun search(keywords: String, offset: Int = 0): Result<SearchResponse> =
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "s" to keywords,
                    "type" to 1,
                    "limit" to 30,
                    "offset" to offset,
                    "total" to true
                )
                val enc = weapi(data)
                val response = api.search(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) Result.Success(body)
                    else Result.Error("搜索结果为空")
                } else {
                    Result.Error("搜索失败: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    suspend fun getHotSearch(): Result<List<HotSearchItem>> =
        withContext(Dispatchers.IO) {
            try {
                val enc = weapi(mapOf("type" to 1111))
                val response = api.getHotSearch(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val items = response.body()?.data ?: emptyList()
                    Result.Success(items)
                } else {
                    Result.Error("获取热搜失败")
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    // ========== 播放 ==========

    suspend fun getSongUrl(songId: Long): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "ids" to "[$songId]",
                    "br" to 999000,
                    "e_r" to false
                )
                val jsonStr = gson.toJson(data)
                val params = NeteaseCrypto.encryptEapi(
                    url = "/api/song/enhance/player/url",
                    dataJson = jsonStr
                )
                // 同一个接口，只是 url 不同
                val response = api.getAccompanyUrl(
                    url = "https://interface.music.163.com/eapi/song/enhance/player/url",
                    params = params
                )
                if (response.isSuccessful) {
                    val url = response.body()?.data?.firstOrNull()?.url
                    if (!url.isNullOrEmpty()) Result.Success(url)
                    else Result.Error("该歌曲暂无版权或无法播放")
                } else {
                    Result.Error("获取播放地址失败: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    // ========== 随心唱伴奏 ==========
    
    suspend fun getSongAccompanyUrl(songId: Long): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val dataMap = mapOf(
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
                    "accompany" to "true",
                    "e_r" to false
                )
                val jsonStr = gson.toJson(dataMap)
                val params = NeteaseCrypto.encryptEapi(
                    url = "/api/song/enhance/player/url/v1",
                    dataJson = jsonStr
                )
                
                val response = api.getAccompanyUrl(params = params)
                if (response.isSuccessful) {
                    val url = response.body()?.data?.firstOrNull()?.url
                    if (!url.isNullOrEmpty()) Result.Success(url)
                    else Result.Error("无法获取伴奏地址")
                } else {
                    Result.Error("请求伴奏失败: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("伴奏连接失败: ${e.message ?: "未知错误"}")
            }
        }

    // ========== 推荐 ==========

    suspend fun getRecommendSongs(): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            try {
                val enc = weapi(emptyMap())
                val response = api.getRecommendSongs(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val songs = response.body()?.data?.dailySongs ?: emptyList()
                    Result.Success(songs)
                } else {
                    Result.Error("获取推荐失败: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    // ========== 歌词 ==========

    suspend fun getLyric(songId: Long): Result<LyricResponse> =
        withContext(Dispatchers.IO) {
            try {
                val data = mapOf(
                    "id" to songId.toString(),
                    "lv" to -1,
                    "kv" to -1,
                    "tv" to -1
                )
                val enc = weapi(data)
                val response = api.getLyric(enc["params"]!!, enc["encSecKey"]!!)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) Result.Success(body)
                    else Result.Error("歌词数据为空")
                } else {
                    Result.Error("获取歌词失败: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }
}
