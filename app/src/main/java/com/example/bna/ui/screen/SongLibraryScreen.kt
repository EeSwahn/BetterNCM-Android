package com.example.bna.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.bna.data.model.Song
import com.example.bna.ui.animation.SaltMotion
import com.example.bna.viewmodel.SearchViewModel

/**
 * 搜索框高度。
 *
 * ★ 注意与 Material3 `TextField` 的最小高度（`TextFieldDefaults.MinHeight` = 56dp）的关系：
 * 若把 TextField 塞进比 56dp 矮的容器且不解除其 `defaultMinSize`，文字会被垂直裁切。
 * 这里保持 48dp 的紧凑外观（对齐盐式），同时在 [SaltSearchBar] 内解除 TextField 的最小高度。
 */
private val SaltSearchBarHeight = 48.dp

/**
 * 「音乐搜索」栏页面 —— 对齐椒盐音乐（Salt Player）的歌曲页骨架。
 *
 * 逆向依据（v11.0.4）：
 *  - 页面脚手架 `aq.m465(title, …)`：标题栏（见 [SaltTopBar]）
 *  - 内容内边距 30dp（`$this$BasicPage` → `m3858(f51Var, 30)`）
 *  - 歌曲列表 `d13.m1337(…, List<Song>, …)`：竖排紧凑列表
 *  - 列表行 `g24.m2112(wa0(title, subtitle))`：双行文本，padding 水平 16dp / 垂直 12dp
 *
 * 本项目为在线网易云搜索，无本地曲库；本页 = 搜索入口 + 搜索结果列表。
 */
@Composable
fun SongLibraryScreen(
    searchViewModel: SearchViewModel,
    onLogout: () -> Unit = {},
    onOpenMenu: (() -> Unit)? = null,
    isTablet: Boolean = true
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val logoutEvent by searchViewModel.logoutEvent.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            searchViewModel.resetLogoutEvent()
            onLogout()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── 标题栏（salt `rx0.m5680`：高 56dp、标题左内边距 16dp）──
        SaltTopBar(
            title = "音乐搜索",
            onOpenMenu = onOpenMenu,
            onLogout = onLogout
        )

        // ── 内容（salt `$this$BasicPage` 内边距 30dp）──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isTablet) 30.dp else 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            SaltSearchBar(
                query = uiState.query,
                onQueryChange = searchViewModel::setQuery,
                onSearch = {
                    searchViewModel.search()
                    focusManager.clearFocus()
                },
                onClear = searchViewModel::clearSearch
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isSearching -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF32C85A))
                        }
                    }
                    uiState.hasSearched -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = uiState.songs,
                                key = { it.id },
                                contentType = { "song" }
                            ) { song ->
                                SongListRow(
                                    song = song,
                                    onClick = { searchViewModel.playSong(song, uiState.songs) },
                                    onPlayNext = { searchViewModel.addSongToPlayNext(song) },
                                    // salt 的列表项出现/重排用"空间"曲线（FastSpatial）
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = SaltMotion.FastEffects,
                                        placementSpec = SaltMotion.FastSpatialOffset,
                                        fadeOutSpec = SaltMotion.FastEffects
                                    )
                                )
                            }
                        }
                    }
                    else -> {
                        EmptyLibraryView()
                    }
                }
            }
        }
    }
}

/**
 * 标题栏 —— salt `rx0.m5681`：高 56dp；左侧 56dp 触摸区菜单/返回按钮；标题左内边距 16dp，单行。
 *
 * @param onLogout 传入时在右侧显示退出登录按钮（仅「音乐搜索」页需要）
 * @param trailing 右侧自定义内容（如歌单页的刷新按钮）；与 [onLogout] 二选一
 */
@Composable
internal fun SaltTopBar(
    title: String,
    onOpenMenu: (() -> Unit)?,
    onLogout: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onOpenMenu != null) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickable(onClick = onOpenMenu),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "菜单",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(16.dp))
        }

        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(16.dp))

        if (trailing != null) {
            trailing()
        } else if (onLogout != null) {
            IconButton(
                onClick = onLogout,
                modifier = Modifier.size(40.dp).clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = "退出登录",
                    tint = Color(0xFF8A8C8E)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun SaltSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SaltSearchBarHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFF8A8C8E),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            // ★ 用 BasicTextField：M3 TextField 内置上下 16dp 内边距，放进 48dp 容器必然裁切文字
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF32C85A)),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearch() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "搜索歌曲、歌手、专辑",
                                color = Color(0xFF6A6C6E),
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                maxLines = 1
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "清除",
                        tint = Color(0xFF6A6C6E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * 歌曲列表行 —— salt `g24.m2112`：封面 + 双行文本（歌名 / 艺术家 · 专辑）+ 尾部操作。
 * padding 水平 16dp / 垂直 12dp。
 */
@Composable
internal fun SongListRow(
    song: Song,
    onClick: () -> Unit,
    onPlayNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumCoverUrl + "?param=120y120",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.05f))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "${song.artistNames} · ${song.albumName}",
                color = Color(0xFF8A8C8E),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = formatSongDuration(song.durationMs),
            color = Color(0xFF6A6C6E),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onPlayNext, modifier = Modifier.size(32.dp)) {
            Icon(
                imageVector = Icons.Rounded.MoreHoriz,
                contentDescription = "下一首播放",
                tint = Color(0xFF8A8C8E),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun EmptyLibraryView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "搜索并播放你喜欢的音乐",
                color = Color(0xFF6A6C6E),
                fontSize = 14.sp
            )
        }
    }
}

internal fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    return String.format("%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}
