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

data class LoginStatusResponse(
    val data: LoginStatusData? = null,
    val code: Int = 0
)

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
