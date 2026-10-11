package com.example.bna.data.model

import com.google.gson.annotations.SerializedName

// ========== 通用 ==========

data class BaseResponse(
    val code: Int,
    val message: String? = null,
    val msg: String? = null
)

// ========== 登录相关 ==========

data class LoginResponse(
    val code: Int,
    val msg: String? = null,
    val message: String? = null,
    val account: Account? = null,
    val profile: Profile? = null,
    val cookie: String? = null
)

data class Account(
    val id: Long,
    val userName: String? = null,
    val status: Int = 0
)

data class Profile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String? = null,
    val signature: String? = null
)

/**
 * 登录状态响应。
 *
 * ★ 注意响应结构：本项目直连官方 `weapi/w/nuser/account/get`，实测该上游响应把
 * `account` / `profile` 放在**顶层**（顶层键只有 `code, account, profile`），
 * **没有 `data` 包裹层**：
 * ```json
 * {"code":200,"account":{"id":...},"profile":{"userId":...,"nickname":"..."}}
 * ```
 * 而 api-enhanced 的 `module/login_status.js` 会在自家服务里把它重新包一层
 * `data`（`body.data = {...result.body}`）后才对外返回 —— 两者形状不同。
 *
 * 历史上本模型只声明了 `data` 包裹形态，导致直连官方时 `data` 恒为 null、
 * `profile` 永远取不到，于是歌单区一直提示「未获取到登录用户信息」。
 * 这里同时保留两套字段并统一由 [profile] / [account] 取值，兼容两种形态。
 */
data class LoginStatusResponse(
    // 官方直连：顶层直接给 account / profile
    val profile: Profile? = null,
    val account: Account? = null,
    // api-enhanced 兼容形态：包在 data 里
    val data: LoginStatusData? = null,
    val code: Int = 0
) {
    /** 统一取 profile：优先顶层（官方直连），回退到 data 包裹层。 */
    val effectiveProfile: Profile? get() = profile ?: data?.profile

    /** 统一取 account：优先顶层（官方直连），回退到 data 包裹层。 */
    val effectiveAccount: Account? get() = account ?: data?.account

    /** 当前登录用户 id；取不到返回 null。 */
    val userId: Long?
        get() = effectiveProfile?.userId?.takeIf { it > 0 }
            ?: effectiveAccount?.id?.takeIf { it > 0 }
}

data class LoginStatusData(
    val profile: Profile? = null,
    val account: Account? = null
)

data class QrKeyResponse(
    val code: Int,
    // PC 接口返回 { data: { unikey: "xxx" } }
    // APP 接口返回 { unikey: "xxx", code: 200 }，直接在根对象里
    val data: QrKeyData? = null,
    val unikey: String? = null
)

data class QrKeyData(
    val unikey: String
)

data class QrCreateResponse(
    val code: Int,
    val data: QrCreateData? = null
)

data class QrCreateData(
    val qrurl: String? = null,
    val qrimg: String? = null
)

data class QrCheckResponse(
    val code: Int,
    val message: String? = null,
    val cookie: String? = null
)

// ========== 搜索相关 ==========

data class SearchResponse(
    val code: Int,
    val result: SearchResult? = null
)

data class SearchResult(
    val songs: List<Song>? = null,
    val songCount: Int = 0
)

data class Song(
    val id: Long,
    val name: String,
    val artists: List<Artist>? = null,
    val ar: List<Artist>? = null,
    val album: Album? = null,
    val al: Album? = null,
    val duration: Long = 0,
    val dt: Long = 0,
    val fee: Int = 0
) {
    val artistNames: String
        get() = (artists ?: ar)?.joinToString(" / ") { it.name } ?: "未知歌手"

    val albumName: String
        get() = (album ?: al)?.name ?: "未知专辑"

    val albumCoverUrl: String
        get() = (album ?: al)?.picUrl?.replace("http://", "https://")?.let { url ->
            if (url.startsWith("https")) url else "https:$url"
        } ?: ""

    val durationMs: Long get() = if (duration > 0) duration else dt
}

data class Artist(
    val id: Long,
    val name: String
)

data class Album(
    val id: Long,
    val name: String,
    val picUrl: String? = null
)

// ========== 播放URL ==========

data class SongUrlResponse(
    val code: Int,
    val data: List<SongUrl>? = null
)

data class SongUrl(
    val id: Long,
    val url: String? = null,
    val code: Int = 0,
    val type: String? = null,
    val br: Int = 0
)

// ========== 推荐/热门 ==========

data class HotSearchResponse(
    val code: Int,
    val data: List<HotSearchItem>? = null
)

data class HotSearchItem(
    val searchWord: String,
    val score: Int = 0,
    val iconType: Int = 0
)

data class RecommendSongsResponse(
    val code: Int,
    val data: RecommendSongsData? = null
)

data class RecommendSongsData(
    val dailySongs: List<Song>? = null
)

// ========== 歌单 ==========

/**
 * 登录用户歌单列表响应。
 *
 * 接口契约来自 NeteaseCloudMusicApiEnhanced（`module/user_playlist.js`）：
 * `POST /api/user/playlist`（weapi），参数 uid / limit / offset / includeVideo。
 * 已用本地 api-enhanced 实测（uid=1）确认字段：顶层 `code` / `more` / `playlist[]`。
 */
data class UserPlaylistResponse(
    val code: Int,
    val more: Boolean = false,
    val playlist: List<Playlist>? = null
)

/**
 * 歌单。
 *
 * 字段取自实测 `playlist[]` 元素（67 个键中本项目所需的子集）；
 * 注意 `trackIds` 在 `/user/playlist` 响应里通常为空数组，
 * 歌曲列表需另调 `playlist_detail` 拿 `trackIds`（详见 PlaylistTrackAllResponse）。
 */
data class Playlist(
    val id: Long,
    val name: String,
    val coverImgUrl: String? = null,
    val trackCount: Int = 0,
    val playCount: Long = 0,
    val description: String? = null,
    val creator: PlaylistCreator? = null,
    /** 是否为当前用户「收藏」的歌单（true）而非自己创建（false） */
    val subscribed: Boolean = false,
    /** 特殊类型：5 = 我喜欢的音乐 */
    val specialType: Int = 0,
    val privacy: Int = 0
) {
    /** 封面统一转 https，兼容网易云返回的 http:// 地址 */
    val coverUrl: String
        get() = coverImgUrl
            ?.replace("http://", "https://")
            ?.let { if (it.startsWith("https")) it else "https:$it" }
            ?: ""

    /** 是否为「我喜欢的音乐」特殊歌单 */
    val isLikedPlaylist: Boolean get() = specialType == 5 || id == 0L
}

data class PlaylistCreator(
    val userId: Long = 0,
    val nickname: String? = null,
    val avatarUrl: String? = null
)

/**
 * `playlist_detail` 响应（`module/playlist_detail.js` → `POST /api/v6/playlist/detail`）。
 * 其中 `trackIds[]` 是歌单内全部歌曲的 ID 序列（实测 id=1 时 158 条），
 * 而 `tracks` 只含前若干首，所以取全量歌曲必须走 trackIds → song/detail。
 */
data class PlaylistDetailResponse(
    val code: Int,
    val playlist: PlaylistDetail? = null
)

data class PlaylistDetail(
    val id: Long = 0,
    val name: String? = null,
    val trackIds: List<PlaylistTrackId>? = null,
    val tracks: List<Song>? = null
)

data class PlaylistTrackId(
    val id: Long,
    val v: Int = 0,
    val at: Long = 0
)

// ========== 歌词 ==========

data class LyricResponse(
    val code: Int,
    val lrc: LrcContent? = null,
    val tlyric: LrcContent? = null,
    val yrc: LrcContent? = null,
    val romalrc: LrcContent? = null,
    val klyric: LrcContent? = null
)

data class LrcContent(
    val lyric: String? = null
)

data class LyricLine(
    val timeMs: Long,
    val text: String,
    val translation: String? = null,
    val durationMs: Long = 2000L,
    val words: List<WordInfo>? = null
)

data class WordInfo(
    val startOffset: Int,
    val duration: Int,
    val text: String
)
