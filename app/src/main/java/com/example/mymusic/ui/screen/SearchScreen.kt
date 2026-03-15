package com.example.mymusic.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mymusic.data.model.Song
import com.example.mymusic.ui.theme.*
import com.example.mymusic.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel(),
    onLogout: () -> Unit = {}
) {
    val uiState by searchViewModel.uiState.collectAsState()
    val playingState by searchViewModel.playingState.collectAsState()
    val logoutEvent by searchViewModel.logoutEvent.collectAsState()
    
    // 监听退出登录事件
    LaunchedEffect(logoutEvent) {
        if (logoutEvent) {
            searchViewModel.resetLogoutEvent()
            onLogout()
        }
    }

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val focusManager = LocalFocusManager.current

    // 背景：深灰到黑的径向渐变，加上彩色的气泡装饰
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // 背景装饰气泡 (Bubbles)
        BackgroundDecoration(isTablet)

        Row(modifier = Modifier.fillMaxSize()) {
            // ── 左侧导航栏 (模仿图中的 Sidebar) ──
            if (isTablet) {
                MusicSidebar()
            }

            // ── 主内容区 ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = if (isTablet) 32.dp else 16.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // 顶部 Tabs: Search | Library
                SearchHeader(onLogout = { searchViewModel.logout() })

                Spacer(modifier = Modifier.height(24.dp))

                // 搜索框：带渐变边框和毛玻璃感
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    MusicSearchBar(
                        query = uiState.query,
                        onQueryChange = searchViewModel::setQuery,
                        onSearch = { searchViewModel.search() },
                        onClear = { searchViewModel.clearSearch() },
                        modifier = Modifier.widthIn(max = 600.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 内容列表
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        uiState.isSearching -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFF5AB9FF))
                            }
                        }
                        uiState.hasSearched -> {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(if (isTablet) 2 else 1),
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.songs, key = { it.id }) { song ->
                                    ModernSongCard(
                                        song = song,
                                        onClick = { searchViewModel.playSong(song) }
                                    )
                                }
                            }
                        }
                        else -> {
                            WelcomeView()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundDecoration(isTablet: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 右上角蓝紫色泡泡
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 5.dp, y = (-20).dp)
                .size(if (isTablet) 300.dp else 200.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF5AB2FF).copy(alpha = 0.2f),
                            Color(0xFF5AB2FF).copy(alpha = 0f)
                        )
                    )
                )
        )
        // 右下角红紫色泡泡
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 20.dp)
                .size(if (isTablet) 350.dp else 250.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFB457FF).copy(alpha = 0.15f),
                            Color(0xFFB457FF).copy(alpha = 0f)
                        )
                    )
                )
        )
        // 左下角深红泡泡
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .size(if (isTablet) 250.dp else 180.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF3060).copy(alpha = 0.12f),
                            Color(0xFFFF3060).copy(alpha = 0f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun MusicSidebar() {
    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(Color(0xFF161819).copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.05f)
            )
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SidebarIcon(Icons.Default.Home, "Home", false)
        Spacer(modifier = Modifier.height(30.dp))
        SidebarIcon(Icons.Default.Search, "Search", true)
        Spacer(modifier = Modifier.height(30.dp))
        SidebarIcon(Icons.Default.LibraryMusic, "Library", false)
        Spacer(modifier = Modifier.height(30.dp))
        SidebarIcon(Icons.Default.Widgets, "Browse", false)
    }
}

@Composable
private fun SidebarIcon(icon: ImageVector, label: String, isActive: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        if (isActive) {
            // 左侧蓝色高亮条
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(4.dp)
                    .height(24.dp)
                    .offset(x = (-16).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF5AB9FF))
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.White else Color(0xFF6A6C6E),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = if (isActive) Color.White else Color(0xFF6A6C6E),
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun SearchHeader(onLogout: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.Bottom) {
        Column {
            Text(
                text = "Search",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(30.dp)
                    .height(2.dp)
                    .background(Color.White)
            )
        }
        Spacer(modifier = Modifier.width(30.dp))
        Text(
            text = "Library",
            color = Color(0xFF6A6C6E),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 退出登录按钮
        IconButton(
            onClick = onLogout,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Icon(
                imageVector = Icons.Outlined.Logout,
                contentDescription = "Logout",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun MusicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    
    // 平滑过渡动画
    val animatedBorderWidth by animateDpAsState(
        targetValue = if (isFocused) 1.5.dp else 1.dp,
        animationSpec = tween(durationMillis = 300),
        label = "borderWidth"
    )
    
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isFocused) 0.08f else 0.05f,
        animationSpec = tween(durationMillis = 300),
        label = "backgroundAlpha"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF8A8C8E),
        animationSpec = tween(durationMillis = 300),
        label = "iconColor"
    )
    
    // 动态边框：通过独立的 Alpha 动画加上 DrawScope 绘制，彻底规避 Brush 重组产生的掉帧
    val animatedBorderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "borderAlpha"
    )

    val gradientBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color(0xFF5AB9FF).copy(alpha = 0.8f), Color(0xFFFF49A1).copy(alpha = 0.8f))
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                color = Color(0xFF2A2C2E),
                shape = RoundedCornerShape(20.dp)
            )
            .drawWithContent {
                drawContent()
                if (animatedBorderAlpha > 0f) {
                    val strokeWidth = animatedBorderWidth.toPx()
                    val halfStroke = strokeWidth / 2f
                    drawRoundRect(
                        brush = gradientBrush,
                        alpha = animatedBorderAlpha,
                        topLeft = androidx.compose.ui.geometry.Offset(halfStroke, halfStroke),
                        size = androidx.compose.ui.geometry.Size(size.width - strokeWidth, size.height - strokeWidth),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx() - halfStroke, 20.dp.toPx() - halfStroke),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                    )
                }
            }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = animatedAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search, 
                contentDescription = null, 
                tint = iconColor, 
                modifier = Modifier.size(20.dp)
            )
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search for artists, songs, albums...", color = Color(0xFF6A6C6E), fontSize = 15.sp) },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { isFocused = it.isFocused },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { 
                    onSearch()
                    focusManager.clearFocus()
                })
            )
            
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF6A6C6E), modifier = Modifier.size(18.dp))
                }
            }
            
            Icon(Icons.Rounded.Mic, null, tint = iconColor, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Icon(Icons.Rounded.FilterList, null, tint = iconColor, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ModernSongCard(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF232527).copy(alpha = 0.7f),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 大尺寸封面
            AsyncImage(
                model = song.albumCoverUrl + "?param=200y200",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            )

            Spacer(modifier = Modifier.width(20.dp))

            // 信息区
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artistNames,
                    color = Color(0xFFB0B0B0),
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Albums • ${song.albumName}",
                    color = Color(0xFF6A6C6E),
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }

            // 控制区
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 播放/暂停按钮 (图中是一个圆形背景的暂停图标)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Icon(
                    imageVector = Icons.Rounded.MoreHoriz,
                    contentDescription = null,
                    tint = Color(0xFF6A6C6E),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // 时长
                Text(
                    text = "3:50", // 假数据，原图中显示在末尾
                    color = Color(0xFF6A6C6E),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun WelcomeView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.QueueMusic, null, tint = Color(0xFF2A2C2E), modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Ready to explore?", color = Color(0xFF6A6C6E), fontSize = 18.sp)
        }
    }
}
