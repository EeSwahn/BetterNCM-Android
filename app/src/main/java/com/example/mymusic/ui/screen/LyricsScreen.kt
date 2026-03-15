package com.example.mymusic.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import android.content.Context
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.mymusic.player.MusicPlayer
import com.example.mymusic.player.PlayerState
import com.example.mymusic.ui.animation.*
import com.example.mymusic.ui.theme.*
import com.example.mymusic.viewmodel.LyricsViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LyricsScreen(
    onDismiss: () -> Unit,
    lyricsViewModel: LyricsViewModel = viewModel()
) {
    val currentSong by remember {
        MusicPlayer.playerState.map { it.currentSong }.distinctUntilChanged()
    }.collectAsState(initial = MusicPlayer.playerState.value.currentSong)
    val lyricsState by lyricsViewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val animationConfig = rememberLyricsAnimationConfig()

    BackHandler {
        lyricsViewModel.hideSuiXinChangSlider()
        onDismiss()
    }

    val song = currentSong

    LaunchedEffect(song?.id) {
        song?.id?.let { lyricsViewModel.loadLyrics(it) }
    }

    LaunchedEffect(Unit) {
        // 动画帧数锁定为60帧 (1000ms / 60fps ≈ 16.66ms)
        val frameDelay = 1000L / 60L
        while (true) {
            delay(frameDelay)
            lyricsViewModel.updateCurrentLine(MusicPlayer.getCurrentPosition())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {} // Intercept background clicks so they don't fall through to MainScreen/PlayerBar
            )
    ) {
        if (song != null) {
            AlbumCoverBackground(
                coverUrl = song.albumCoverUrl,
                animationConfig = animationConfig
            )
        }

        if (isTablet) {
            TabletLyricsLayout(
                currentSong = song,
                lyricsState = lyricsState,
                onDismiss = onDismiss,
                lyricsViewModel = lyricsViewModel,
                animationConfig = animationConfig
            )
        } else {
            PhoneLyricsLayout(
                currentSong = song,
                lyricsState = lyricsState,
                onDismiss = onDismiss,
                lyricsViewModel = lyricsViewModel,
                animationConfig = animationConfig
            )
        }
    }
}

@Composable
private fun AlbumCoverBackground(
    coverUrl: String,
    animationConfig: LyricsAnimationConfig = rememberLyricsAnimationConfig()
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (coverUrl.isNotEmpty()) {
            AsyncImage(
                model = coverUrl + "?param=800y800",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(
                        radius = 100.dp,
                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun TabletLyricsLayout(
    currentSong: com.example.mymusic.data.model.Song?,
    lyricsState: com.example.mymusic.viewmodel.LyricsUiState,
    onDismiss: () -> Unit,
    lyricsViewModel: LyricsViewModel,
    animationConfig: LyricsAnimationConfig
) {
    val song = currentSong ?: return
    var verticalScrollSpeed by rememberFloatPreference("verticalScrollSpeed", 1.0f)
    var scaleAnimationSpeed by rememberFloatPreference("scaleAnimationSpeed", 1.0f)
    var activeLyricSizeRatio by rememberFloatPreference("activeLyricSizeRatio", 1.0f)
    var baseFontSizeRatio by rememberFloatPreference("baseFontSizeRatio", 1.0f)
    var lineSpacingRatio by rememberFloatPreference("lineSpacingRatio", 0.7f)
    var showSettings by remember { mutableStateOf(false) }

    var headerOffsetX by rememberFloatPreference("headerOffsetX", 0f)
    var headerOffsetY by rememberFloatPreference("headerOffsetY", 0f)
    var coverOffsetX by rememberFloatPreference("coverOffsetX", 0f)
    var coverOffsetY by rememberFloatPreference("coverOffsetY", 0f)
    var audioSpecOffsetX by rememberFloatPreference("audioSpecOffsetX", 0f)
    var audioSpecOffsetY by rememberFloatPreference("audioSpecOffsetY", 0f)
    var playbackOffsetX by rememberFloatPreference("playbackOffsetX", 0f)
    var playbackOffsetY by rememberFloatPreference("playbackOffsetY", 0f)
    var bottomOffsetX by rememberFloatPreference("bottomOffsetX", 0f)
    var bottomOffsetY by rememberFloatPreference("bottomOffsetY", 0f)
    var lyricsPanelOffsetX by rememberFloatPreference("lyricsPanelOffsetX", 0f)
    var lyricsPanelOffsetY by rememberFloatPreference("lyricsPanelOffsetY", 0f)
    
    var progressBarOffsetX by rememberFloatPreference("progressBarOffsetX", 0f)
    var progressBarOffsetY by rememberFloatPreference("progressBarOffsetY", 0f)
    var progressBarWidthRatio by rememberFloatPreference("progressBarWidthRatio", 1.0f)
    var progressBarHeight by rememberFloatPreference("progressBarHeight", 4.0f)
    var progressBarThumbSize by rememberFloatPreference("progressBarThumbSize", 20.0f)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(80.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // Calculate a safe width bounding box to ensure Cover, Slider, and texts share the exact same bounds
            val nonCoverHeight = 360.dp 
            val idealWidth = minOf(maxWidth * 0.95f, maxHeight - nonCoverHeight).coerceAtLeast(200.dp)

            Column(
                modifier = Modifier.width(idealWidth).fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().offset(x = headerOffsetX.dp, y = headerOffsetY.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.name,
                            color = TextPrimary,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artistNames,
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                    Icon(Icons.Default.Podcasts, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(x = coverOffsetX.dp, y = coverOffsetY.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (song.albumCoverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = song.albumCoverUrl + "?param=800y800",
                            contentDescription = song.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(DarkCard),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeteaseRed, modifier = Modifier.size(80.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "AudioTrack    FLAC 16 bits    48 kHz",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth().offset(x = audioSpecOffsetX.dp, y = audioSpecOffsetY.dp),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProgressBarOnly(
                    offsetX = progressBarOffsetX,
                    offsetY = progressBarOffsetY,
                    widthRatio = progressBarWidthRatio
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.offset(x = playbackOffsetX.dp, y = playbackOffsetY.dp)) {
                    PlaybackButtonsOnly(isPhone = false)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                BottomActionButtons(
                    isPhone = false,
                    modifier = Modifier.offset(x = bottomOffsetX.dp, y = bottomOffsetY.dp),
                    onSettingsClick = { showSettings = !showSettings }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .offset(x = lyricsPanelOffsetX.dp, y = lyricsPanelOffsetY.dp)
                ) {
                    LyricsPanel(
                        lyricsState = lyricsState,
                        lyricsViewModel = lyricsViewModel,
                        onDismiss = onDismiss,
                        animationConfig = animationConfig,
                        verticalScrollSpeed = verticalScrollSpeed,
                        scaleAnimationSpeed = scaleAnimationSpeed,
                        activeLyricSizeRatio = activeLyricSizeRatio,
                        baseFontSizeRatio = baseFontSizeRatio,
                        lineSpacingRatio = lineSpacingRatio
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .graphicsLayer { clip = false },
                    horizontalArrangement = Arrangement.End
                ) {
                    SuiXinChangButton(
                        currentSong = song,
                        lyricsViewModel = lyricsViewModel,
                        isPhone = false
                    )
                }
                AnimatedVisibility(visible = showSettings) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .padding(horizontal = 32.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingSliderRow("滚动速度", verticalScrollSpeed, { verticalScrollSpeed = it }, 0.1f..1.0f, 8)
                        SettingSliderRow("缩放速度", scaleAnimationSpeed, { scaleAnimationSpeed = it }, 0.1f..1.0f, 8)
                        SettingSliderRow("居中放大", activeLyricSizeRatio, { activeLyricSizeRatio = it }, 0.1f..1.0f, 8)
                        SettingSliderRow("所有字号", baseFontSizeRatio, { baseFontSizeRatio = it }, 0.5f..2.0f, 15)
                        SettingSliderRow("歌词行距", lineSpacingRatio, { lineSpacingRatio = it }, 0.5f..3.0f, 25)
                        SettingSliderRow("标题X", headerOffsetX, { headerOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("标题Y", headerOffsetY, { headerOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("封面X", coverOffsetX, { coverOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("封面Y", coverOffsetY, { coverOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("音质X", audioSpecOffsetX, { audioSpecOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("音质Y", audioSpecOffsetY, { audioSpecOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("控制X", playbackOffsetX, { playbackOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("控制Y", playbackOffsetY, { playbackOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("底部X", bottomOffsetX, { bottomOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("底部Y", bottomOffsetY, { bottomOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("歌词X", lyricsPanelOffsetX, { lyricsPanelOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("歌词Y", lyricsPanelOffsetY, { lyricsPanelOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("进度条X", progressBarOffsetX, { progressBarOffsetX = it }, -200f..200f, 0)
                        SettingSliderRow("进度条Y", progressBarOffsetY, { progressBarOffsetY = it }, -200f..200f, 0)
                        SettingSliderRow("进度条宽", progressBarWidthRatio, { progressBarWidthRatio = it }, 0.3f..2.0f, 17)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneLyricsLayout(
    currentSong: com.example.mymusic.data.model.Song?,
    lyricsState: com.example.mymusic.viewmodel.LyricsUiState,
    onDismiss: () -> Unit,
    lyricsViewModel: LyricsViewModel,
    animationConfig: LyricsAnimationConfig
) {
    val song = currentSong ?: return
    var verticalScrollSpeed by rememberFloatPreference("verticalScrollSpeed", 1.0f)
    var scaleAnimationSpeed by rememberFloatPreference("scaleAnimationSpeed", 1.0f)
    var activeLyricSizeRatio by rememberFloatPreference("activeLyricSizeRatio", 1.0f)
    var baseFontSizeRatio by rememberFloatPreference("baseFontSizeRatio", 1.0f)
    var lineSpacingRatio by rememberFloatPreference("lineSpacingRatio", 0.7f)
    var showSettings by remember { mutableStateOf(false) }

    // 是否显示歌词视图（初始为 false，即封面视图）
    var showLyrics by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部栏：关闭按钮 + (歌词模式下显示) 歌曲信息
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "关闭",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            AnimatedVisibility(
                visible = showLyrics,
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally()
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = song.name,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = song.artistNames,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // 中间主内容区域：可点击切换
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showLyrics = !showLyrics }
                )
        ) {
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500))
                },
                label = "lyrics_content_toggle"
            ) { displayLyrics ->
                if (displayLyrics) {
                    // 歌词视图 (对应横屏右半部分)
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LyricsPanel(
                                lyricsState = lyricsState,
                                lyricsViewModel = lyricsViewModel,
                                onDismiss = onDismiss,
                                isPhone = true,
                                animationConfig = animationConfig,
                                verticalScrollSpeed = verticalScrollSpeed,
                                scaleAnimationSpeed = scaleAnimationSpeed,
                                activeLyricSizeRatio = activeLyricSizeRatio,
                                baseFontSizeRatio = baseFontSizeRatio,
                                lineSpacingRatio = lineSpacingRatio
                            )
                        }
                        
                        // 随心唱按钮，放在歌词下方
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .graphicsLayer { clip = false },
                            horizontalArrangement = Arrangement.End
                        ) {
                            SuiXinChangButton(
                                currentSong = song,
                                lyricsViewModel = lyricsViewModel,
                                isPhone = true
                            )
                        }
                    }
                } else {
                    // 封面视图 (对应横屏左半部分)
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 歌曲名与歌手
                        Text(
                            text = song.name,
                            color = TextPrimary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = song.artistNames,
                            color = TextSecondary,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // 方形封面 (参考 TabletLyricsLayout)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .shadow(
                                    elevation = 24.dp,
                                    shape = RoundedCornerShape(12.dp),
                                    spotColor = Color.Black.copy(alpha = 0.6f)
                                )
                        ) {
                            if (song.albumCoverUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = song.albumCoverUrl + "?param=800y800",
                                    contentDescription = song.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(DarkCard),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = NeteaseRed,
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // 音质信息
                        Text(
                            text = "AudioTrack    FLAC 16 bits    48 kHz",
                            color = TextTertiary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // 设置区域 (可选显示)
        AnimatedVisibility(visible = showSettings) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SettingSliderRow("滚动速度", verticalScrollSpeed, { verticalScrollSpeed = it }, 0.1f..1.0f, 8)
                SettingSliderRow("缩放速度", scaleAnimationSpeed, { scaleAnimationSpeed = it }, 0.1f..1.0f, 8)
                SettingSliderRow("居中放大", activeLyricSizeRatio, { activeLyricSizeRatio = it }, 0.1f..1.0f, 8)
                SettingSliderRow("所有字号", baseFontSizeRatio, { baseFontSizeRatio = it }, 0.5f..2.0f, 15)
                SettingSliderRow("歌词行距", lineSpacingRatio, { lineSpacingRatio = it }, 0.5f..3.0f, 25)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 播放控制
        PlaybackControls(isPhone = true)

        Spacer(modifier = Modifier.height(16.dp))

        // 底部动作栏
        BottomActionButtons(
            isPhone = true,
            onSettingsClick = { showSettings = !showSettings }
        )
    }
}

@Composable
private fun AlbumCoverPanel(
    coverUrl: String,
    songName: String,
    artistName: String,
    isPhone: Boolean = true
) {
    val isPlaying by remember {
        MusicPlayer.playerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = MusicPlayer.playerState.value.isPlaying)
    
    // 动画帧数锁定为60帧: 手动计算旋转角度，限制刷新率为 60fps
    var rotation by remember { mutableStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val frameDelay = 1000L / 60L
            val rotationPerFrame = 360f / (20000f / frameDelay) // 20000ms rotating 360 degrees
            while (true) {
                delay(frameDelay)
                rotation = (rotation + rotationPerFrame) % 360f
            }
        }
    }

    val coverSize = 240.dp

    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(coverSize).clip(CircleShape).shadow(
                    elevation = 16.dp,
                    shape = CircleShape,
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverUrl + "?param=600y600",
                    contentDescription = songName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationZ = if (isPlaying) rotation else 0f
                        }
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkCard),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeteaseRed,
                        modifier = Modifier.size(coverSize / 3)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(coverSize / 4)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(DarkBackground)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = songName,
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artistName,
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun LyricsPanel(
    lyricsState: com.example.mymusic.viewmodel.LyricsUiState,
    lyricsViewModel: LyricsViewModel,
    onDismiss: () -> Unit,
    isPhone: Boolean = false,
    animationConfig: LyricsAnimationConfig = rememberLyricsAnimationConfig(),
    verticalScrollSpeed: Float = 1.0f,
    scaleAnimationSpeed: Float = 1.0f,
    activeLyricSizeRatio: Float = 1.0f,
    baseFontSizeRatio: Float = 1.0f,
    lineSpacingRatio: Float = 0.7f
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lyricsState.currentLineIndex) {
        val activeIndex = lyricsState.currentLineIndex
        if (activeIndex >= 0) {
            var layoutInfo = listState.layoutInfo
            var viewportHeight = layoutInfo.viewportSize.height
            if (viewportHeight == 0) {
                delay(50)
                layoutInfo = listState.layoutInfo
                viewportHeight = layoutInfo.viewportSize.height
            }
            if (viewportHeight == 0) return@LaunchedEffect

            var activeItemInfo = layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
            
            if (activeItemInfo == null) {
                val jumpIndex = (activeIndex - if (isPhone) 3 else 2).coerceAtLeast(0)
                listState.scrollToItem(jumpIndex)
                delay(30)
                layoutInfo = listState.layoutInfo
                activeItemInfo = layoutInfo.visibleItemsInfo.find { it.index == activeIndex }
            }

            if (activeItemInfo != null) {
                val activeCenter = activeItemInfo!!.offset + (activeItemInfo!!.size / 2)
                // 视觉中心稍微往上移动，大概放置在屏幕偏上的 38% 位置（歌词界面标准光学中心）
                val viewportCenter = (viewportHeight * 0.38f).toInt()
                val distanceToScroll = activeCenter - viewportCenter
                
                val duration = (500 / verticalScrollSpeed).toInt().coerceAtLeast(100)
                listState.animateScrollBy(
                    value = distanceToScroll.toFloat(),
                    animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    when {
        lyricsState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        }
        lyricsState.hasNoLyric || lyricsState.lyrics.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无歌词",
                    color = TextTertiary,
                    fontSize = 16.sp
                )
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(vertical = if (isPhone) 80.dp else 120.dp),
                horizontalAlignment = if (isPhone) Alignment.CenterHorizontally else Alignment.Start,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = lyricsState.lyrics,
                    key = { index, line -> "${line.timeMs}_$index" }
                ) { index, line ->
                    val distance = Math.abs(index - lyricsState.currentLineIndex)
                    LyricLineItem(
                        line = line,
                        isCurrent = index == lyricsState.currentLineIndex,
                        distance = distance,
                        scaleAnimationSpeed = scaleAnimationSpeed,
                        activeLyricSizeRatio = activeLyricSizeRatio,
                        baseFontSizeRatio = baseFontSizeRatio,
                        lineSpacingRatio = lineSpacingRatio,
                        onClick = {
                            MusicPlayer.seekTo(line.timeMs)
                        },
                        isPhone = isPhone
                    )
                }
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: com.example.mymusic.data.model.LyricLine,
    isCurrent: Boolean,
    distance: Int,
    scaleAnimationSpeed: Float = 1.0f,
    activeLyricSizeRatio: Float = 1.0f,
    baseFontSizeRatio: Float = 1.0f,
    lineSpacingRatio: Float = 0.7f,
    onClick: () -> Unit,
    isPhone: Boolean = false
) {
    val isPlaying by remember {
        MusicPlayer.playerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = MusicPlayer.playerState.value.isPlaying)
    val animationConfig = rememberLyricsAnimationConfig()
    
    var isPressed by remember { mutableStateOf(false) }

    val animationDuration = (400 / scaleAnimationSpeed).toInt().coerceAtLeast(100)
    
    val fontEasing = FastOutSlowInEasing
    
    val baseActiveFontSize = if (isPhone) 26f else 44f
    val baseActiveLineHeight = if (isPhone) 36f else 60f
    
    val inactiveScale = if (isPhone) 16f / 26f else 26f / 44f
    val targetScale = if (isCurrent) activeLyricSizeRatio else inactiveScale
    
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = animationDuration, easing = fontEasing),
        label = "scale"
    )
    
    val fontSize = baseFontSizeRatio * baseActiveFontSize
    val lineHeight = baseFontSizeRatio * baseActiveLineHeight
    
    val targetAlpha = if (isCurrent) 1f else 0.3f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    // Removed font layout animation. Hardware acceleration handling scaling below.
    
    val textColor by animateColorAsState(
        targetValue = if (isCurrent) Color.White else TextSecondary,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "textColor"
    )

    Column(
        horizontalAlignment = if (isPhone) Alignment.CenterHorizontally else Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
            .let { modifier ->
                if (animationConfig.enable3DEffect) {
                    modifier.lyrics3DEffect(isCurrent, distance)
                } else {
                    modifier
                }
            }
            .let { modifier ->
                if (animationConfig.enableFlowingLight) {
                    modifier.flowingLightEffect(
                        isCurrent = isCurrent,
                        isPlaying = isPlaying,
                        reduceEffect = animationConfig.reduceFlowingLightEffect
                    )
                } else {
                    modifier
                }
            }
            .let { modifier ->
                if (animationConfig.enableGlowEffect) {
                    modifier.glowEffect(
                        isCurrent = isCurrent,
                        glowColor = Color.White
                    )
                } else {
                    modifier
                }
            }
            .clickBounceEffect(isPressed)
            .padding(
                horizontal = if (isPhone) 16.dp else 32.dp,
                vertical = (if (isPhone) 4.dp else 4.dp) * lineSpacingRatio
            )
            .graphicsLayer {
                this.alpha = alpha
                this.scaleX = scale
                this.scaleY = scale
                this.transformOrigin = if (isPhone) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0f, 0.5f)
            }
    ) {
        Text(
            text = line.text,
            color = textColor,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            textAlign = if (isPhone) TextAlign.Center else TextAlign.Start,
            lineHeight = lineHeight.sp,
            modifier = Modifier.padding(vertical = 4.dp * lineSpacingRatio)
        )

        line.translation?.let { translation ->
            if (isCurrent) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = translation,
                    color = NeteaseRed.copy(alpha = 0.8f),
                    fontSize = if (isPhone) 14.sp else 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PlaybackControlsOverlay(
    playerState: PlayerState,
    onDismiss: () -> Unit
) {
    // Left empty or removed since Tablet layout now uses Native controls
    // It is still used by PhoneLyricsLayout though? No, PhoneLyricsLayout uses PlaybackControls directly.
    // Wait, TabletLyricsLayout DOES NOT use PlaybackControlsOverlay anymore.
    // What about PhoneLyricsLayout?
    // Let's check PhoneLyricsLayout, it calls `PlaybackControls(playerState = playerState, isPhone = true)`.
    // So PlaybackControlsOverlay is actually not used anywhere anymore! 
    // I can just make it an empty Composable or delete it. Let's make it a no-op just in case.
}

@Composable
private fun CustomProgressBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.3f))
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth(value)
                .height(2.dp)
                .align(Alignment.CenterStart)
                .background(Color.White)
        )
        
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0f),
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                thumbColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ProgressBarOnly(
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    widthRatio: Float = 1.0f
) {
    val playerState by MusicPlayer.playerState.collectAsState()
    val rawProgress = if (playerState.duration > 0) {
        (playerState.currentPosition.toFloat() / playerState.duration).coerceIn(0f, 1f)
    } else 0f
    
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp, y = offsetY.dp)
    ) {
        var isDragging by remember { mutableStateOf(false) }
        var dragProgress by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier.fillMaxWidth(widthRatio)
        ) {
            CustomProgressBar(
                value = if (isDragging) dragProgress else progress,
                onValueChange = { 
                    isDragging = true
                    dragProgress = it 
                },
                onValueChangeFinished = {
                    isDragging = false
                    val targetMs = (dragProgress * playerState.duration).toLong()
                    MusicPlayer.seekTo(targetMs)
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(widthRatio),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val currentDisplayMs = if (isDragging) {
                (dragProgress * playerState.duration).toLong()
            } else {
                playerState.currentPosition
            }
            Text(
                text = formatTime(currentDisplayMs),
                color = TextTertiary,
                fontSize = 12.sp
            )
            Text(
                text = formatTime(playerState.duration),
                color = TextTertiary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PlaybackButtonsOnly(
    isPhone: Boolean = false
) {
    val playerState by MusicPlayer.playerState.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isPhone) 32.dp else 40.dp, Alignment.CenterHorizontally)
    ) {
        IconButton(
            onClick = { MusicPlayer.seekTo(0) },
            modifier = Modifier.size(if (isPhone) 48.dp else 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "上一首",
                tint = TextPrimary,
                modifier = Modifier.size(if (isPhone) 28.dp else 32.dp)
            )
        }

        IconButton(
            onClick = { MusicPlayer.togglePlayPause() },
            modifier = Modifier
                .size(if (isPhone) 72.dp else 72.dp)
        ) {
            Icon(
                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(if (isPhone) 48.dp else 56.dp)
            )
        }

        IconButton(
            onClick = { /* Next song logic not implemented yet */ }, 
            modifier = Modifier.size(if (isPhone) 48.dp else 48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = "下一首",
                tint = TextPrimary,
                modifier = Modifier.size(if (isPhone) 28.dp else 32.dp)
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    isPhone: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        ProgressBarOnly()
        
        Spacer(modifier = Modifier.height(if (isPhone) 20.dp else 24.dp))
        
        PlaybackButtonsOnly(
            isPhone = isPhone
        )
    }
}

@Composable
private fun BottomActionButtons(
    isPhone: Boolean = false,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isPhone) 8.dp else 0.dp)
            .padding(bottom = if (isPhone) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionButton(Icons.Default.Repeat, "循环模式", isPhone)
        ActionButton(Icons.Default.Timer, "定时关闭", isPhone)
        ActionButton(Icons.Default.Tune, "设置", isPhone, onClick = onSettingsClick)
        ActionButton(Icons.Default.List, "播放列表", isPhone)
        ActionButton(Icons.Default.MoreHoriz, "更多", isPhone)
    }
}

@Composable
private fun SuiXinChangButton(
    currentSong: com.example.mymusic.data.model.Song?,
    lyricsViewModel: LyricsViewModel,
    isPhone: Boolean = false
) {
    val uiState by lyricsViewModel.state.collectAsState()

    // Use ViewModel-owned slider visibility so it can be reset externally
    // (e.g. when LyricsScreen is dismissed mid-exit-animation)
    val showSlider = uiState.suiXinChangSliderVisible
    fun setShowSlider(v: Boolean) {
        if (v) lyricsViewModel.showSuiXinChangSlider()
        else   lyricsViewModel.hideSuiXinChangSlider()
    }

    // Track previous state — only show slider when transitioning false → true
    var prevActive by remember { mutableStateOf(uiState.suiXinChangActive) }
    LaunchedEffect(uiState.suiXinChangActive) {
        if (uiState.suiXinChangActive && !prevActive) {
            lyricsViewModel.showSuiXinChangSlider()
        }
        prevActive = uiState.suiXinChangActive
    }

    // 3s inactivity → hide slider
    LaunchedEffect(showSlider, uiState.suiXinChangVolume) {
        if (showSlider) {
            delay(3000)
            lyricsViewModel.hideSuiXinChangSlider()
        }
    }

    val displaySlider = uiState.suiXinChangActive && showSlider

    val animatedVolume by animateFloatAsState(
        targetValue = uiState.suiXinChangVolume,
        animationSpec = tween(80, easing = LinearEasing),
        label = "volumeBar"
    )

    // Button fades out when slider shows; pulses slightly when active
    val iconAlpha by animateFloatAsState(
        targetValue = if (displaySlider) 0f else 1f,
        animationSpec = tween(200),
        label = "iconAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (uiState.suiXinChangActive && !displaySlider) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )

    val buttonSizeDp = if (isPhone) 40.dp else 48.dp

    // ── 1. The button — lives in normal layout, always receives touches ──
    Box {
        IconButton(
            onClick = {
                if (currentSong != null) {
                    if (uiState.suiXinChangActive) {
                        setShowSlider(true)
                    } else {
                        lyricsViewModel.toggleSuiXinChang(currentSong)
                    }
                }
            },
            modifier = Modifier.size(buttonSizeDp)
        ) {
            if (uiState.suiXinChangLoading) {
                CircularProgressIndicator(
                    color = NeteaseRed,
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(iconAlpha)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "随心唱",
                    tint = if (uiState.suiXinChangActive) NeteaseRed.copy(alpha = 0.85f) else TextSecondary.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(if (isPhone) 24.dp else 28.dp)
                        .alpha(iconAlpha)
                        .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                )
            }
        }

        // ── 2. Slider — Popup window above layout ──
        if (displaySlider) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -(buttonSizeDp.value.toInt() + 8))
            ) {
                // Animate from 0→1 by flipping targetAlpha on first composition
                var targetAlpha by remember { mutableStateOf(0f) }
                var targetOffsetY by remember { mutableStateOf(20f) }
                    LaunchedEffect(Unit) {
                        targetAlpha = 1f
                        targetOffsetY = 0f
                    }
                val popupAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(220),
                    label = "popupAlpha"
                )
                val popupOffsetY by animateFloatAsState(
                    targetValue = targetOffsetY,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                    label = "popupOffY"
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .width(28.dp)
                        .height(120.dp)
                        .graphicsLayer {
                            alpha = popupAlpha
                            translationY = popupOffsetY.dp.toPx()
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard.copy(alpha = 0.72f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val totalHeight = size.height.toFloat()
                                onValueChangeWithShowSlider(
                                    lyricsViewModel,
                                    (1f - (down.position.y / totalHeight)).coerceIn(0f, 1f),
                                    { setShowSlider(true) }
                                )
                                var pointer = down
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val current = event.changes.firstOrNull { it.id == pointer.id }
                                    if (current != null && current.pressed) {
                                        pointer = current
                                        onValueChangeWithShowSlider(
                                            lyricsViewModel,
                                            (1f - (current.position.y / totalHeight)).coerceIn(0f, 1f),
                                            { setShowSlider(true) }
                                        )
                                        current.consume()
                                    } else break
                                }
                            }
                        }
                ) {
                    val activeHeight = maxHeight * animatedVolume
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(activeHeight)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(NeteaseRed.copy(alpha = 0.6f), NeteaseRed.copy(alpha = 0.88f))
                                )
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    isPhone: Boolean,
    onClick: () -> Unit = { /* TODO: 功能先不写 */ }
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (isPhone) 40.dp else 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(if (isPhone) 24.dp else 28.dp)
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun onValueChangeWithShowSlider(
    lyricsViewModel: LyricsViewModel,
    value: Float,
    showSliderAction: () -> Unit
) {
    lyricsViewModel.setSuiXinChangVolume(value)
    showSliderAction()
}

@Composable
private fun rememberFloatPreference(key: String, defaultValue: Float): MutableState<Float> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lyrics_settings", Context.MODE_PRIVATE) }
    
    val state = remember { mutableStateOf(prefs.getFloat(key, defaultValue)) }
    
    return remember {
        object : MutableState<Float> {
            override var value: Float
                get() = state.value
                set(v) {
                    state.value = v
                    prefs.edit().putFloat(key, v).apply()
                }
            override fun component1() = state.value
            override fun component2(): (Float) -> Unit = { v: Float -> value = v }
        }
    }
}

@Composable
private fun SettingSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value.toString()) }
    
    val displayText = when {
        label.contains("X") || label.contains("Y") -> String.format(java.util.Locale.US, "%.0f", value)
        label.contains("宽") || label.contains("高") || label.contains("大小") -> String.format(java.util.Locale.US, "%.0f", value)
        else -> String.format(java.util.Locale.US, "%.1fx", value)
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.width(90.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                activeTrackColor = NeteaseRed,
                thumbColor = NeteaseRed,
                inactiveTrackColor = DarkCard
            )
        )
        Text(
            text = displayText,
            color = TextPrimary,
            fontSize = 14.sp,
            modifier = Modifier
                .width(50.dp)
                .clickable {
                    tempValue = value.toString()
                    showDialog = true
                }
                .background(DarkCard, RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            textAlign = TextAlign.Center
        )
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("设置 $label", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    label = { Text("数值") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NeteaseRed,
                        unfocusedLabelColor = TextSecondary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newValue = tempValue.toFloatOrNull()
                        if (newValue != null) {
                            onValueChange(newValue.coerceIn(valueRange))
                        }
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = DarkBackground
        )
    }
}
