package com.example.mymusic.data.network

import com.example.mymusic.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface NeteaseApiService {

    @FormUrlEncoded
    @POST("weapi/sms/captcha/sent")
    suspend fun sendCaptcha(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<BaseResponse>

    @FormUrlEncoded
    @POST("weapi/login/cellphone")
    suspend fun loginByPhone(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("weapi/w/nuser/account/get")
    suspend fun getLoginStatus(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<LoginStatusResponse>

    @FormUrlEncoded
    @POST("weapi/logout")
    suspend fun logout(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<LoginResponse>

    @FormUrlEncoded
    @POST("weapi/login/qrcode/unikey")
    suspend fun getQrKey(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String,
        @Header("X-Identity") identity: String? = null
    ): Response<QrKeyResponse>

    @FormUrlEncoded
    @POST("weapi/login/qrcode/create")
    suspend fun createQrCode(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String,
        @Header("X-Identity") identity: String? = null
    ): Response<QrCreateResponse>

    @FormUrlEncoded
    @POST("weapi/login/qrcode/check")
    suspend fun checkQrStatus(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String,
        @Header("X-Identity") identity: String? = null
    ): Response<QrCheckResponse>

    @FormUrlEncoded
    @POST("weapi/login/qrcode/client/login")
    suspend fun checkQrStatusMobile(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String,
        @Header("X-Identity") identity: String? = null
    ): Response<QrCheckResponse>

    @FormUrlEncoded
    @POST("weapi/cloudsearch/get/web")
    suspend fun search(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<SearchResponse>

    @FormUrlEncoded
    @POST("weapi/search/hot")
    suspend fun getHotSearch(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<HotSearchResponse>

    @FormUrlEncoded
    @POST("weapi/song/enhance/player/url/v1")
    suspend fun getSongUrl(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<SongUrlResponse>

    @FormUrlEncoded
    @POST
    suspend fun getAccompanyUrl(
        @Url url: String = "https://interface3.music.163.com/eapi/song/enhance/player/url/v1",
        @Field("params") params: String
    ): Response<SongUrlResponse>

    @FormUrlEncoded
    @POST("weapi/v3/song/detail")
    suspend fun getSongDetail(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<SongDetailResponse>

    @FormUrlEncoded
    @POST("weapi/v3/discovery/recommend/songs")
    suspend fun getRecommendSongs(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<RecommendSongsResponse>

    @FormUrlEncoded
    @POST("weapi/song/lyric")
    suspend fun getLyric(
        @Field("params") params: String,
        @Field("encSecKey") encSecKey: String
    ): Response<LyricResponse>
}

data class SongDetailResponse(
    val code: Int,
    val songs: List<com.example.mymusic.data.model.Song>? = null
)
