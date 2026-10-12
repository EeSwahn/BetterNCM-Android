package com.example.bna.ui.screen

import android.widget.Toast
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import coil.compose.AsyncImage
import com.example.bna.player.MusicPlayer
import kotlin.math.roundToInt
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val playerState by MusicPlayer.playerState.collectAsState()
    val homeColors = rememberHomeColors(playerState.currentSong?.albumCoverUrl.orEmpty())
    val flight = remember { CoverFlight() }
    val reveal = remember { Animatable(if (showLyricsScreen) 1f else 0f) }
    var rootOrigin by remember { mutableStateOf(Offset.Zero) }
    var miniBarBounds by remember { mutableStateOf<Rect?>(null) }
    LaunchedEffect(showLyricsScreen) {
        if (showLyricsScreen) {
            // Wait for the destination's real layout, including user cover calibration.
            var frames = 0
            while (flight.expanded == null && frames++ < 20) withFrameNanos { }
        }
        flight.active = flight.mini != null && flight.expanded != null
        try {
            reveal.animateTo(if (showLyricsScreen) 1f else 0f,
                tween(if (showLyricsScreen) 520 else 420, easing = HomeEase))
        } finally {
            flight.active = false
        }
        if (!showLyricsScreen) flight.expanded = null
    }
    LaunchedEffect(playerState.currentSong?.id) {
        if (playerState.currentSong == null) showLyricsScreen = false
    }
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = (if (playerState.currentSong != null) 112.dp else 24.dp) + navigationBottom
    CompositionLocalProvider(LocalHomeColors provides homeColors, LocalCoverFlight provides flight,
        LocalHomeListBottomPadding provides listBottomPadding) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(homeColors.background).onGloballyPositioned { rootOrigin = it.positionInRoot() }) {
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
                        isTablet = true
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
                        drawerContainerColor = homeColors.background
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
                    isTablet = false
                )
            }
        }

        // A single floating bar is centered in the entire main window.
        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            .padding(horizontal = if (isTablet) 32.dp else 16.dp, vertical = 20.dp)) {
            Box(Modifier.widthIn(max = 620.dp)
                .onGloballyPositioned { miniBarBounds = it.boundsInRoot() }
                .graphicsLayer { alpha = (1f - reveal.value * 5f).coerceIn(0f, 1f) }
                .shadow(24.dp, RoundedCornerShape(24.dp))) {
                PlayerBar(onClick = { showLyricsScreen = true })
            }
        }

        // Keep the player at its final layout size; only its visible shell expands.
        // This preserves stable cover coordinates and the existing calibrated layout.
        if (showLyricsScreen || reveal.value > 0f) {
            val source = miniBarBounds
            Box(Modifier.fillMaxSize().drawWithContent {
                val fraction = reveal.value
                val full = Rect(0f, 0f, size.width, size.height)
                val origin = source?.translate(-rootOrigin) ?: Rect(0f, size.height, size.width, size.height)
                val shell = androidx.compose.ui.geometry.lerp(origin, full, fraction)
                val corner = 24.dp.toPx() * (1f - fraction)
                val mask = Path().apply { addRoundRect(RoundRect(shell, CornerRadius(corner))) }
                clipPath(mask) {
                    drawRect(Brush.horizontalGradient(listOf(
                        androidx.compose.ui.graphics.lerp(homeColors.surface, homeColors.background, fraction),
                        androidx.compose.ui.graphics.lerp(
                            androidx.compose.ui.graphics.lerp(homeColors.surface, homeColors.accent, 0.09f),
                            homeColors.background, fraction))))
                    this@drawWithContent.drawContent()
                }
            }) {
                Box(Modifier.fillMaxSize().graphicsLayer {
                    // The expanding card appears first, followed by player controls.
                    alpha = ((reveal.value - 0.12f) / 0.68f).coerceIn(0f, 1f)
                }) {
                    LyricsScreen(onDismiss = { showLyricsScreen = false })
                }
            }
        }
        val start = flight.mini
        val end = flight.expanded
        if (flight.active && start != null && end != null) {
            val fraction = reveal.value
            val rect = androidx.compose.ui.geometry.lerp(start, end, fraction)
            val density = LocalDensity.current
            val radius = if (maxWidth >= 600.dp) 8f else 22f
            Box(Modifier
                .offset { IntOffset((rect.left - rootOrigin.x).roundToInt(), (rect.top - rootOrigin.y).roundToInt()) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .clip(RoundedCornerShape((12f + (radius - 12f) * fraction).dp))
                .background(homeColors.surface), contentAlignment = Alignment.Center) {
                AsyncImage(model = playerState.currentSong?.albumCoverUrl?.plus("?param=800y800"),
                    placeholder = flight.artwork, error = flight.artwork,
                    contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
    }
}

/** 右侧栏目铺满主界面，列表末尾的滚动留白由各页面处理。 */
@Composable
private fun MainContent(
    selectedId: String,
    playlists: List<com.example.bna.data.model.Playlist>,
    playlistViewModel: PlaylistViewModel,
    searchViewModel: SearchViewModel,
    onLogout: () -> Unit,
    onOpenMenu: (() -> Unit)?,
    isTablet: Boolean
) {
    val colors = LocalHomeColors.current
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.lerp(colors.background, colors.surface, 0.45f), colors.background)))) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = selectedId,
                modifier = Modifier.fillMaxSize().clipToBounds(),
                transitionSpec = {
                    // Both pages move together, without fades or size animation.
                    val motion = tween<androidx.compose.ui.unit.IntOffset>(
                        durationMillis = 340,
                        easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
                    )
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, motion)
                        .togetherWith(slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, motion))
                        .using(null)
                },
                label = "libraryNavigation"
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
