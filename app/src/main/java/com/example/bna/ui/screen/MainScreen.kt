package com.example.bna.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bna.ui.animation.SaltMotion
import com.example.bna.ui.theme.DarkBackground
import com.example.bna.viewmodel.LoginViewModel
import com.example.bna.viewmodel.PlaylistViewModel
import com.example.bna.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

/**
 * 主容器 —— 采用椒盐音乐（Salt Player）平板主界面骨架。
 *
 * 逆向依据（v11.0.4 `androidx.core.za0` case 1 / `xq3.m7194` / `j94.m3097`）：
 * ```
 * BoxWithConstraints
 *   isTablet = maxWidth >= 640dp
 *   左栏宽度 = isTablet ? (maxWidth < 1000dp ? 280 : 360) : 200
 *   ├ 平板：左栏常驻（SidebarMenu） + 右内容
 *   └ 手机：抽屉（ModalNavigationDrawer，宽度 200dp）
 * ```
 * 底部迷你播放条与歌词页弹出沿用本项目原有行为。
 *
 * 栏目按需求精简为：「音乐搜索」+ 登录用户的每个歌单（各占左栏一栏）。
 */
@Composable
fun MainScreen(
    onLogout: () -> Unit = {},
    loginViewModel: LoginViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    var showLyricsScreen by rememberSaveable { mutableStateOf(false) }
    var selectedId by rememberSaveable { mutableStateOf("search") }

    val playlistViewModel: PlaylistViewModel = viewModel()
    val playlistState by playlistViewModel.uiState.collectAsState()

    val context = LocalContext.current

    // 进入主界面即拉取登录用户歌单
    LaunchedEffect(Unit) {
        playlistViewModel.loadPlaylists()
    }

    /**
     * 退出登录。
     *
     * 这里必须真正作废会话，而不仅仅是跳转页面：
     * 1. `searchViewModel.logout()` → `repository.logout()` 会先 `ApiClient.beginLogout()`
     *    立即清掉本地 Cookie，再调服务端 `/weapi/logout`，最后 `endLogout()`。
     * 2. 同步重置 [LoginViewModel]（Activity 作用域，启动时把 `isLoggedIn` 置成了 true）。
     *
     * 修复前这里只做了「清 UI 状态 + 跳转」，**从未清 Cookie**：
     * 结果 `LoginScreen` 一挂载 `hasActiveSession()` 仍为 true，立刻又跳回主界面
     * （「登录页闪一下就没了」），用户根本无法退出登录。
     */
    val scope = rememberCoroutineScope()
    val handleLogout: () -> Unit = {
        scope.launch {
            // 1) 真正退出：清 Cookie + 作废服务端会话
            searchViewModel.logoutAndAwait()
            // 2) 清歌单缓存，避免账号串数据
            playlistViewModel.clear()
            // 3) 重置登录页状态，否则登录页会读到陈旧的 isLoggedIn=true 又弹回来
            loginViewModel.onLoggedOut()
            Toast.makeText(context, "已退出登录", Toast.LENGTH_SHORT).show()
            // 4) 最后才导航
            onLogout()
        }
    }

    // 从主界面返回（如按返回键）时，若 Cookie 已失效则同步一次登录态，避免状态漂移
    DisposableEffect(Unit) {
        onDispose {
            if (!loginViewModel.hasActiveSession()) {
                loginViewModel.onLoggedOut()
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // salt: isTablet = maxWidth >= 640dp
        val isTablet = maxWidth >= 640.dp
        val sidebarWidth = when {
            !isTablet -> 200
            maxWidth < 1000.dp -> 280
            else -> 360
        }

        val drawerState = rememberDrawerState(DrawerValue.Closed)

        val openMenu: () -> Unit = {
            scope.launch { drawerState.open() }
        }

        if (isTablet) {
            // ── 平板：左栏常驻 + 右内容 ──
            Row(modifier = Modifier.fillMaxSize()) {
                SidebarMenu(
                    selectedId = selectedId,
                    onSelect = { selectedId = it },
                    width = sidebarWidth,
                    playlists = playlistState.playlists,
                    playlistsLoading = playlistState.isLoading,
                    playlistError = playlistState.errorMessage,
                    onRefreshPlaylists = { playlistViewModel.loadPlaylists(force = true) }
                )

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    MainContent(
                        selectedId = selectedId,
                        playlists = playlistState.playlists,
                        playlistViewModel = playlistViewModel,
                        searchViewModel = searchViewModel,
                        onLogout = handleLogout,
                        onOpenMenu = null,
                        isTablet = true,
                        onOpenLyrics = { showLyricsScreen = true }
                    )
                }
            }
        } else {
            // ── 手机：抽屉 ──
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(sidebarWidth.dp),
                        drawerContainerColor = DarkBackground
                    ) {
                        SidebarMenu(
                            selectedId = selectedId,
                            onSelect = {
                                selectedId = it
                                scope.launch { drawerState.close() }
                            },
                            width = sidebarWidth,
                            playlists = playlistState.playlists,
                            playlistsLoading = playlistState.isLoading,
                            playlistError = playlistState.errorMessage,
                            onRefreshPlaylists = { playlistViewModel.loadPlaylists(force = true) }
                        )
                    }
                }
            ) {
                MainContent(
                    selectedId = selectedId,
                    playlists = playlistState.playlists,
                    playlistViewModel = playlistViewModel,
                    searchViewModel = searchViewModel,
                    onLogout = handleLogout,
                    onOpenMenu = openMenu,
                    isTablet = false,
                    onOpenLyrics = { showLyricsScreen = true }
                )
            }
        }

        // 歌词页全屏弹出（原有行为，曲线换成 salt 的"空间"规格）
        AnimatedVisibility(
            visible = showLyricsScreen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = SaltMotion.SlowSpatialOffset
            ) + fadeIn(animationSpec = SaltMotion.DefaultEffects),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = SaltMotion.FastSpatialOffset
            ) + fadeOut(animationSpec = SaltMotion.FastEffects)
        ) {
            LyricsScreen(onDismiss = { showLyricsScreen = false })
        }
    }
}

/** 右侧内容：底部播放条 + 当前栏目页面。 */
@Composable
private fun MainContent(
    selectedId: String,
    playlists: List<com.example.bna.data.model.Playlist>,
    playlistViewModel: PlaylistViewModel,
    searchViewModel: SearchViewModel,
    onLogout: () -> Unit,
    onOpenMenu: (() -> Unit)?,
    isTablet: Boolean,
    onOpenLyrics: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            PlayerBar(onClick = onOpenLyrics)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // ── 栏目切换：salt 用 Crossfade（`g52.java:28` → `aq.m468`，
            //    规格 `a61.DefaultEffects` = spring(1.0f, 1600f)）──
            Crossfade(
                targetState = selectedId,
                animationSpec = SaltMotion.pageSwitch,
                label = "Crossfade"
            ) { currentId ->
                when {
                    currentId == "search" -> SongLibraryScreen(
                        searchViewModel = searchViewModel,
                        onLogout = onLogout,
                        onOpenMenu = onOpenMenu,
                        isTablet = isTablet
                    )

                    currentId.startsWith("playlist_") -> {
                        val pid = currentId.removePrefix("playlist_").toLongOrNull()
                        PlaylistDetailScreen(
                            playlist = playlists.firstOrNull { it.id == pid },
                            playlistId = pid,
                            onOpenMenu = onOpenMenu,
                            isTablet = isTablet,
                            playlistViewModel = playlistViewModel
                        )
                    }

                    else -> SongLibraryScreen(
                        searchViewModel = searchViewModel,
                        onLogout = onLogout,
                        onOpenMenu = onOpenMenu,
                        isTablet = isTablet
                    )
                }
            }
        }
    }
}
