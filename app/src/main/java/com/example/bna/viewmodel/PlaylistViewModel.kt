package com.example.bna.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bna.data.model.Playlist
import com.example.bna.data.model.Song
import com.example.bna.data.repository.MusicRepository
import com.example.bna.data.repository.PlaylistLoadException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 左侧栏 / 歌单页状态。 */
data class PlaylistUiState(
    val isLoading: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val errorMessage: String? = null,
    /** 当前打开的详情页状态（未打开时为 null） */
    val detail: PlaylistDetailUiState? = null
)

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val isLoading: Boolean = false,
    val songs: List<Song> = emptyList(),
    val errorMessage: String? = null,
    /** 数据来自缓存（用于判断是否需要后台刷新） */
    val fromCache: Boolean = false,
    /** 缓存已过期，正在后台刷新 */
    val isRefreshing: Boolean = false
)

/**
 * 登录用户歌单。
 *
 * 接口依据：NeteaseCloudMusicApiEnhanced `module/user_playlist.js`
 * （`/api/user/playlist`，weapi）+ `module/playlist_track_all.js`（两步取全量歌曲）。
 *
 * 缓存策略（内存 LRU + 磁盘 JSON 两级，见 [com.example.bna.data.cache.MusicCache]）：
 *  - **命中且新鲜**：直接回显，不发任何请求；
 *  - **命中但过期**：**先回显旧数据**（不转圈），再后台静默刷新，刷新成功后原地替换；
 *  - **未命中**：走网络，显示加载态；
 *  - **网络失败但有旧缓存**：继续显示旧数据（不打断浏览）。
 */
class PlaylistViewModel : ViewModel() {

    private val repository = MusicRepository()

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    private var loaded = false

    /**
     * 拉取登录用户歌单。
     *
     * @param force 忽略缓存强制刷新（下拉/点刷新按钮时用）
     */
    fun loadPlaylists(force: Boolean = false) {
        if (loaded && !force) return
        viewModelScope.launch {
            // 已有数据时不显示整页 loading，避免闪烁
            val hasData = _uiState.value.playlists.isNotEmpty()
            _uiState.value = _uiState.value.copy(
                isLoading = !hasData,
                errorMessage = null
            )
            try {
                val result = repository.getMyPlaylistsCached(
                    allowStale = true,
                    forceRefresh = force
                )
                loaded = true
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    playlists = result.value,
                    errorMessage = null
                )
                // 过期缓存：后台再刷一次，拿到新数据后原地替换
                if (result.fromCache && result.isStale) {
                    runCatching { repository.getMyPlaylistsCached(forceRefresh = true) }
                        .onSuccess { fresh ->
                            _uiState.value = _uiState.value.copy(
                                playlists = fresh.value,
                                errorMessage = null
                            )
                        }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "获取歌单失败"
                )
            }
        }
    }

    /**
     * 打开某个歌单。
     *
     * 缓存命中时**立即显示**（无加载态）；否则显示加载态并请求网络。
     * 过期缓存会先展示再后台刷新，保证「秒开」。
     */
    fun openPlaylist(playlist: Playlist) {
        viewModelScope.launch {
            // 1) 先查缓存，命中就秒开、完全不闪 loading
            val cached = com.example.bna.data.cache.MusicCache.readPlaylistSongs(playlist.id)
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    detail = PlaylistDetailUiState(
                        playlist = playlist,
                        songs = cached.value,
                        fromCache = true,
                        isRefreshing = !cached.isFresh,
                        isLoading = false
                    )
                )
                // 新鲜则直接结束；过期则继续往下走后台刷新
                if (cached.isFresh) return@launch
            } else {
                _uiState.value = _uiState.value.copy(
                    detail = PlaylistDetailUiState(playlist = playlist, isLoading = true)
                )
            }

            // 2) 网络获取（过期刷新 或 未命中）
            try {
                val result = repository.getPlaylistSongsCached(
                    playlistId = playlist.id,
                    allowStale = true,
                    forceRefresh = true
                )
                _uiState.value = _uiState.value.copy(
                    detail = PlaylistDetailUiState(
                        playlist = playlist,
                        songs = result.value,
                        fromCache = result.fromCache,
                        isLoading = false
                    )
                )
            } catch (e: PlaylistLoadException) {
                val cur = _uiState.value.detail
                // 已有缓存数据时不打断浏览，只记错误
                _uiState.value = _uiState.value.copy(
                    detail = PlaylistDetailUiState(
                        playlist = playlist,
                        songs = cur?.songs.orEmpty(),
                        errorMessage = if (cur?.songs.isNullOrEmpty()) e.message else null,
                        isLoading = false
                    )
                )
            }
        }
    }

    /** 强制刷新当前打开的歌单（标题栏刷新按钮）。 */
    fun refreshCurrentPlaylist() {
        val pl = _uiState.value.detail?.playlist ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                detail = _uiState.value.detail?.copy(isRefreshing = true, errorMessage = null)
            )
            try {
                val result = repository.getPlaylistSongsCached(
                    playlistId = pl.id,
                    forceRefresh = true
                )
                _uiState.value = _uiState.value.copy(
                    detail = PlaylistDetailUiState(
                        playlist = pl,
                        songs = result.value,
                        isLoading = false
                    )
                )
            } catch (e: PlaylistLoadException) {
                _uiState.value = _uiState.value.copy(
                    detail = _uiState.value.detail?.copy(
                        isRefreshing = false,
                        errorMessage = e.message
                    )
                )
            }
        }
    }

    fun closeDetail() {
        _uiState.value = _uiState.value.copy(detail = null)
    }

    /**
     * 退出登录时清空。
     *
     * 同时清掉账号相关的磁盘缓存，避免下一个账号看到上一个账号的歌单。
     */
    fun clear() {
        loaded = false
        _uiState.value = PlaylistUiState()
        com.example.bna.data.cache.MusicCache.clearUserData()
    }
}
