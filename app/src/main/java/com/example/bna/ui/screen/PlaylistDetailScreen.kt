package com.example.bna.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bna.data.model.Playlist
import com.example.bna.player.MusicPlayer
import com.example.bna.viewmodel.PlaylistViewModel

/**
 * 歌单详情页 —— 点左侧栏某个歌单后，右侧展示该歌单的全部歌曲。
 *
 * 接口依据（NeteaseCloudMusicApiEnhanced）：
 *  - `module/playlist_detail.js` → `/api/v6/playlist/detail` 取全量 `trackIds`
 *  - `module/playlist_track_all.js` → 再用 `/api/v3/song/detail` 批量换歌曲详情
 * 两步都在 [com.example.bna.data.repository.MusicRepository.getPlaylistSongs] 内完成。
 *
 * 视觉沿用 [SaltTopBar] / [SongListRow]（salt `rx0.m5681` / `g24.m2112`）。
 *
 * @param playlist 已知的歌单元数据；为 null 时按 [playlistId] 兜底（歌单列表尚未加载的场景）
 */
@Composable
fun PlaylistDetailScreen(
    playlist: Playlist? = null,
    playlistId: Long? = null,
    onOpenMenu: (() -> Unit)? = null,
    isTablet: Boolean = true,
    playlistViewModel: PlaylistViewModel = viewModel()
) {
    val state by playlistViewModel.uiState.collectAsState()
    val detail = state.detail

    // 若由 selectedId 恢复进来，先补拉歌单详情
    val targetId = playlist?.id ?: playlistId
    LaunchedEffect(targetId) {
        if (targetId != null && targetId != detail?.playlist?.id) {
            val known = playlist ?: state.playlists.firstOrNull { it.id == targetId }
            if (known != null) {
                playlistViewModel.openPlaylist(known)
            }
        }
    }

    val current = detail?.playlist ?: playlist
    val title = current?.name ?: "歌单"

    Column(modifier = Modifier.fillMaxSize()) {
        SaltTopBar(
            title = title,
            onOpenMenu = onOpenMenu,
            trailing = {
                // 加载/后台刷新时显示小转圈；否则给一个手动刷新按钮
                if (detail?.isLoading == true || detail?.isRefreshing == true) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF32C85A),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = { playlistViewModel.refreshCurrentPlaylist() },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "重新加载",
                            tint = Color(0xFF8A8C8E)
                        )
                    }
                }
            }
        )

        // 歌单元信息行：封面 + 曲目数 / 播放量 / 创建者
        if (current != null) {
            PlaylistMetaRow(playlist = current, isTablet = isTablet)
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                detail?.isLoading == true && detail.songs.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF32C85A))
                    }
                }

                detail?.errorMessage != null && detail.songs.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = detail.errorMessage ?: "加载失败",
                            color = Color(0xFFE0533D),
                            fontSize = 13.sp
                        )
                    }
                }

                detail?.songs.isNullOrEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (detail == null) "正在加载歌单…" else "该歌单暂无歌曲",
                            color = Color(0xFF6A6C6E),
                            fontSize = 14.sp
                        )
                    }
                }

                else -> {
                    val songs = detail?.songs.orEmpty()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = if (isTablet) 30.dp else 8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(songs, key = { it.id }) { song ->
                            SongListRow(
                                song = song,
                                onClick = { MusicPlayer.playSongs(songs, songs.indexOf(song)) },
                                onPlayNext = { MusicPlayer.enqueueNext(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 歌单元信息（曲目数 / 播放量 / 创建者）。 */
@Composable
private fun PlaylistMetaRow(playlist: Playlist, isTablet: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isTablet) 30.dp else 16.dp,
                end = if (isTablet) 30.dp else 16.dp,
                top = 4.dp,
                bottom = 8.dp
            )
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val parts = buildList {
            add("${playlist.trackCount} 首")
            if (playlist.playCount > 0) add("播放 ${formatCount(playlist.playCount)}")
            playlist.creator?.nickname?.takeIf { it.isNotBlank() }?.let { add("by $it") }
        }
        Text(
            text = parts.joinToString(" · "),
            color = Color(0xFF6A6C6E),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Normal
        )
    }
}

/** 播放量简写（1.2万 / 3.4亿），与网易云客户端观感一致。 */
private fun formatCount(count: Long): String = when {
    count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
    count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
    else -> count.toString()
}
