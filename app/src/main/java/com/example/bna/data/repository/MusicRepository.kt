package com.example.bna.data.repository

import com.example.bna.data.cache.MusicCache
import com.example.bna.data.model.*
import com.example.bna.data.network.ApiClient
import com.example.bna.util.NeteaseCrypto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int = -1) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

/**
 * 带来源标记的结果，用于「缓存优先」的读取路径。
 *
 * @param fromCache 本次数据是否来自缓存
 * @param isStale   是否已过期（通常配合后台刷新使用）
 */
data class CachedResult<T>(
    val value: T,
    val fromCache: Boolean,
    val isStale: Boolean
)

/** 歌单加载失败（供缓存路径抛出，避免嵌套 Result）。 */
class PlaylistLoadException(message: String) : Exception(message)

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

    /**
     * 退出登录。
     *
     * 顺序很关键（这是「登录页闪一下就没了」的根因）：
     * 1. 先 `beginLogout()` —— 置位退出标志并立即清 Cookie。标志会让 [ApiClient] 的
     *    拦截器在整个退出期间拒绝落盘任何 `Set-Cookie`；
     * 2. 再调服务端 `/weapi/logout` 作废会话（失败也无所谓，本地已经清了）；
     * 3. 最后 `endLogout()` 复位标志，并再清一次兜底。
     *
     * 修复前是「先请求、后清 Cookie」，而该请求自身会经过拦截器，其响应里的
     * `Set-Cookie: MUSIC_U=...` 会把刚清掉的登录态**又写回来**，导致退出实际未生效。
     */
    suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.IO) {
            ApiClient.beginLogout()
            try {
                val enc = weapi(emptyMap())
                api.logout(enc["params"]!!, enc["encSecKey"]!!)
            } catch (e: Exception) {
                // 服务端作废失败不影响本地退出
                println("DEBUG Repository: server logout failed (ignored): ${e.message}")
            } finally {
                ApiClient.clearCookie()
                ApiClient.endLogout()
            }
            Result.Success(Unit)
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

    // ========== 歌单 ==========

    /**
     * 获取指定用户的歌单列表。
     *
     * 契约来自 NeteaseCloudMusicApiEnhanced `module/user_playlist.js`：
     * ```js
     * data = { uid, limit: query.limit || 30, offset: query.offset || 0, includeVideo: true }
     * request('/api/user/playlist', data, 'weapi')
     * ```
     * 本方法分页拉取直到 `more == false`（默认上限 5 页 / 每页 100，足够覆盖常见账号）。
     */
    suspend fun getUserPlaylists(
        uid: Long,
        limit: Int = 100,
        fetchAll: Boolean = true
    ): Result<List<Playlist>> =
        withContext(Dispatchers.IO) {
            try {
                val all = mutableListOf<Playlist>()
                var offset = 0
                var page = 0
                val maxPages = 5
                while (true) {
                    val data = mapOf(
                        "uid" to uid.toString(),
                        "limit" to limit.toString(),
                        "offset" to offset.toString(),
                        "includeVideo" to "true"
                    )
                    val enc = weapi(data)
                    val response = api.getUserPlaylist(enc["params"]!!, enc["encSecKey"]!!)
                    if (!response.isSuccessful) {
                        if (all.isEmpty()) {
                            return@withContext Result.Error("获取歌单失败: ${response.code()}", response.code())
                        }
                        break
                    }
                    val body = response.body() ?: break
                    all += body.playlist.orEmpty()
                    page++
                    val more = body.more
                    if (!fetchAll || !more || page >= maxPages) break
                    offset += limit
                }

                // 「我喜欢的音乐」置顶，其余保持服务端顺序
                val sorted = all.sortedByDescending { it.isLikedPlaylist }
                Result.Success(sorted)
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    /**
     * 获取当前登录用户的歌单列表（uid 取自已保存的登录信息）。
     *
     * 注意：uid 本身**不缓存**（必须先从登录态拿到 uid 才知道缓存 key 与账号归属）；
     * 歌单列表结果按 uid 缓存。
     */
    suspend fun getMyPlaylists(): Result<List<Playlist>> {
        val status = getLoginStatus()
        return when (status) {
            is Result.Success -> {
                // 官方直连时 account/profile 在顶层；兼容 data 包裹形态（见 LoginStatusResponse）
                val uid = status.data.userId
                if (uid == null) {
                    Result.Error("未获取到登录用户信息，请重新登录")
                } else {
                    getUserPlaylists(uid)
                }
            }
            is Result.Error -> Result.Error(status.message, status.code)
            else -> Result.Error("未登录")
        }
    }

    /**
     * 用户歌单列表的缓存版读取。
     *
     * @param allowStale 允许过期时先回显旧数据
     * @param forceRefresh 忽略缓存强制走网络
     */
    suspend fun getMyPlaylistsCached(
        allowStale: Boolean = true,
        forceRefresh: Boolean = false
    ): CachedResult<List<Playlist>> = withContext(Dispatchers.IO) {
        // 先确认 uid（登录态不缓存）
        val status = getLoginStatus()
        val uid = when (status) {
            is Result.Success -> status.data.userId
            is Result.Error -> throw PlaylistLoadException(status.message)
            else -> null
        } ?: throw PlaylistLoadException("未获取到登录用户信息，请重新登录")

        if (!forceRefresh) {
            val hit = MusicCache.readMyPlaylists(uid)
            if (hit != null && (hit.isFresh || allowStale)) {
                return@withContext CachedResult(hit.value, fromCache = true, isStale = !hit.isFresh)
            }
        }

        when (val r = getUserPlaylists(uid)) {
            is Result.Success -> {
                MusicCache.writeMyPlaylists(uid, r.data)
                CachedResult(r.data, fromCache = false, isStale = false)
            }
            is Result.Error -> {
                val stale = MusicCache.readMyPlaylists(uid)
                if (stale != null) {
                    CachedResult(stale.value, fromCache = true, isStale = true)
                } else {
                    throw PlaylistLoadException(r.message)
                }
            }
            else -> throw PlaylistLoadException("加载失败")
        }
    }

    /**
     * 获取歌单内全部歌曲（**带缓存**）。
     *
     * 两步（对齐 api-enhanced `module/playlist_track_all.js`）：
     * 1. `playlist/detail`（`/api/v6/playlist/detail`）拿 `trackIds[]` 全量 ID；
     * 2. `song/detail`（`/api/v3/song/detail`，参数 `c=[{"id":...}]`）批量换歌曲详情。
     *
     * 实测：id=1 的 trackIds 有 158 条，而 `tracks` 只含前段，故必须走此两步。
     * 歌曲详情接口每次最多 1000 个 ID，这里按 500 分批。
     *
     * 缓存：命中直连返回；过期则**先返回旧数据**（调用方可用 [forceRefresh] 拿新数据）。
     */
    suspend fun getPlaylistSongs(playlistId: Long, limit: Int = 1000): Result<List<Song>> =
        withContext(Dispatchers.IO) {
            fetchPlaylistSongs(playlistId, limit)
        }

    /**
     * 取歌单歌曲，区分「缓存」与「网络」两条来源。
     *
     * @param allowStale 允许在缓存过期时先回显旧数据（用于秒开 + 后台刷新）
     * @param forceRefresh 忽略缓存强制走网络
     */
    suspend fun getPlaylistSongsCached(
        playlistId: Long,
        limit: Int = 1000,
        allowStale: Boolean = true,
        forceRefresh: Boolean = false
    ): CachedResult<List<Song>> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val hit = MusicCache.readPlaylistSongs(playlistId)
            if (hit != null && (hit.isFresh || allowStale)) {
                return@withContext CachedResult(
                    value = hit.value,
                    fromCache = true,
                    isStale = !hit.isFresh
                )
            }
        }
        when (val r = fetchPlaylistSongs(playlistId, limit)) {
            is Result.Success -> {
                MusicCache.writePlaylistSongs(playlistId, r.data)
                CachedResult(r.data, fromCache = false, isStale = false)
            }
            is Result.Error -> {
                // 网络失败时兜底用过期缓存，避免用户看到空白
                val stale = MusicCache.readPlaylistSongs(playlistId)
                if (stale != null) {
                    CachedResult(stale.value, fromCache = true, isStale = true)
                } else {
                    throw PlaylistLoadException(r.message)
                }
            }
            else -> throw PlaylistLoadException("加载失败")
        }
    }

    /** 纯网络获取（缓存未命中或强制刷新时走这里）。 */
    private suspend fun fetchPlaylistSongs(playlistId: Long, limit: Int): Result<List<Song>> {
        return try {
            val detailEnc = weapi(
                mapOf(
                    "id" to playlistId.toString(),
                    "n" to "100000",
                    "s" to "8"
                )
            )
            val detailResp = api.getPlaylistDetail(detailEnc["params"]!!, detailEnc["encSecKey"]!!)
            if (!detailResp.isSuccessful) {
                return Result.Error("获取歌单详情失败: ${detailResp.code()}", detailResp.code())
            }
            val detail = detailResp.body()?.playlist
                ?: return Result.Error("歌单数据为空")

            val ids = detail.trackIds.orEmpty().map { it.id }.take(limit)
            if (ids.isEmpty()) {
                // 回退：极少数歌单 detail 不返回 trackIds，此时只能用内嵌 tracks
                return Result.Success(detail.tracks.orEmpty())
            }

            val songs = mutableListOf<Song>()
            ids.chunked(500).forEach { chunk ->
                val c = chunk.joinToString(prefix = "[", postfix = "]") { "{\"id\":$it}" }
                val enc = weapi(mapOf("c" to c))
                val resp = api.getSongDetail(enc["params"]!!, enc["encSecKey"]!!)
                if (resp.isSuccessful) {
                    songs += resp.body()?.songs.orEmpty()
                }
            }
            Result.Success(songs)
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
                // 使用 EAPI 并模拟 Android 身份以获取 YRC (逐字) 歌词
                val data = mapOf(
                    "id" to songId.toString(),
                    "cp" to false,
                    "tv" to -1,
                    "lv" to -1,
                    "rv" to -1,
                    "kv" to -1,
                    "yv" to -1,
                    "ytv" to -1,
                    "yrv" to -1
                )
                val jsonStr = gson.toJson(data)
                val params = NeteaseCrypto.encryptEapi(
                    url = "/api/song/lyric/v1",
                    dataJson = jsonStr
                )
                
                val response = api.getLyricEapi(params = params)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        Result.Error("歌词数据为空")
                    } else if (body.tlyric?.lyric.isNullOrBlank() && !body.lrc?.lyric.isNullOrBlank()) {
                        // v1 接口对部分歌曲/游客身份不返回 tlyric，回退旧的 weapi 歌词接口补取翻译
                        val translated = fetchLegacyTlyric(songId)
                        if (translated.isNullOrBlank()) {
                            Result.Success(body)
                        } else {
                            Result.Success(body.copy(tlyric = com.example.bna.data.model.LrcContent(translated)))
                        }
                    } else {
                        Result.Success(body)
                    }
                } else {
                    Result.Error("获取歌词失败: ${response.code()}", response.code())
                }
            } catch (e: Exception) {
                Result.Error("连接失败: ${e.message ?: "未知错误"}")
            }
        }

    /** 旧版 weapi 歌词接口兜底获取翻译，失败返回 null（不影响主歌词流程） */
    private suspend fun fetchLegacyTlyric(songId: Long): String? = try {
        val enc = weapi(mapOf("id" to songId.toString(), "lv" to -1, "kv" to -1, "tv" to -1))
        val resp = api.getLyric(enc["params"]!!, enc["encSecKey"]!!)
        if (resp.isSuccessful) resp.body()?.tlyric?.lyric?.takeIf { it.isNotBlank() } else null
    } catch (e: Exception) {
        null
    }
}
