package com.example.bna.ui.screen.lyrics

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.data.model.Song
import com.example.bna.ui.animation.LyricsAnimationConfig
import com.example.bna.ui.theme.DarkCard
import com.example.bna.ui.theme.NeteaseRed
import com.example.bna.ui.theme.TextPrimary
import com.example.bna.ui.theme.TextSecondary
import com.example.bna.ui.theme.TextTertiary
import com.example.bna.viewmodel.LyricsUiState
import com.example.bna.viewmodel.LyricsViewModel
import kotlinx.coroutines.delay

@Composable
fun PhoneLyricsLayout(
    currentSong: Song?,
    lyricsState: LyricsUiState,
    onDismiss: () -> Unit,
    lyricsViewModel: LyricsViewModel,
    animationConfig: LyricsAnimationConfig
) {
    val song = currentSong ?: return
    var verticalScrollSpeed by rememberFloatPreference("verticalScrollSpeed", 0.5f)
    var scaleAnimationSpeed by rememberFloatPreference("scaleAnimationSpeed", 0.5f)
    var activeLyricSizeRatio by rememberFloatPreference("activeLyricSizeRatio", 0.7f)
    var baseFontSizeRatio by rememberFloatPreference("baseFontSizeRatio", 1.3f)
    var lineSpacingRatio by rememberFloatPreference("lineSpacingRatio", 0.7f)
    var enableWordByWord by rememberBooleanPreference("enableWordByWord", true)
    var yrcFloatSpeed by rememberFloatPreference("yrcFloatSpeed", 0.3f)
    var yrcFloatIntensity by rememberFloatPreference("yrcFloatIntensity", 12f)
    var wordTimingOffsetMs by rememberFloatPreference("wordTimingOffsetMs", 0f)
    var wordScaleSpeed by rememberFloatPreference("wordScaleSpeed", 0.4f)
    var wordScaleSize by rememberFloatPreference("wordScaleSize", 1.0f)
    var showSettings by remember { mutableStateOf(false) }
    var lyricsControlsVisible by remember { mutableStateOf(true) }
    var lyricsInteractionVersion by remember { mutableStateOf(0) }

    val pagerState = rememberPagerState(initialPage = 0) { 2 }
    val currentPage by remember {
        derivedStateOf { pagerState.settledPage }
    }
    val showLyrics by remember {
        derivedStateOf { currentPage == 1 }
    }
    fun registerLyricsInteraction() {
        if (showLyrics && !showSettings) {
            lyricsControlsVisible = true
            lyricsInteractionVersion++
        }
    }

    LaunchedEffect(showLyrics, showSettings, lyricsInteractionVersion) {
        if (showLyrics && !showSettings) {
            lyricsControlsVisible = true
            delay(3000)
            lyricsControlsVisible = false
        } else {
            lyricsControlsVisible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(showLyrics, showSettings) {
                if (!showLyrics || showSettings) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    registerLyricsInteraction()
                }
            }
            .padding(top = 24.dp, bottom = 24.dp, start = 12.dp, end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "关闭",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                transitionSpec = {
                    fadeIn() + slideInHorizontally { it / 4 } togetherWith
                        fadeOut() + slideOutHorizontally { -it / 4 }
                },
                label = "phone_header_content"
            ) { page ->
                if (page == 1) {
                    Column {
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
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = song.name,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = song.artistNames,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(40.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            if (page == 1) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
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
                            lineSpacingRatio = lineSpacingRatio,
                            enableWordByWord = enableWordByWord,
                            yrcFloatSpeed = yrcFloatSpeed,
                            yrcFloatIntensity = yrcFloatIntensity,
                            wordTimingOffsetMs = wordTimingOffsetMs,
                            wordScaleSpeed = wordScaleSpeed,
                            wordScaleSize = wordScaleSize
                        )
                    }
                    AnimatedVisibility(
                        visible = lyricsControlsVisible,
                        enter = fadeIn(animationSpec = tween(220)) + slideInVertically(animationSpec = tween(220)) { it / 2 },
                        exit = fadeOut(animationSpec = tween(220)) + slideOutVertically(animationSpec = tween(220)) { it / 2 }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuiXinChangButton(
                                currentSong = song,
                                lyricsViewModel = lyricsViewModel,
                                isPhone = true
                            )
                        }
                    }
                }
            } else {
                PhoneAlbumPage(song = song)
            }
        }

        AnimatedContent(
            targetState = showLyrics && !lyricsControlsVisible,
            transitionSpec = {
                fadeIn(animationSpec = tween(240)) + slideInVertically(animationSpec = tween(240)) { it / 3 } togetherWith
                    fadeOut(animationSpec = tween(240)) + slideOutVertically(animationSpec = tween(240)) { it / 3 }
            },
            label = "phone_lyrics_bottom_controls"
        ) { collapsed ->
            if (collapsed) {
                ProgressBarOnly()
            } else {
                Column {
                    Spacer(modifier = Modifier.height(18.dp))
                    PlaybackControls(isPhone = true)
                    Spacer(modifier = Modifier.height(12.dp))
                    BottomActionButtons(
                        isPhone = true,
                        onSettingsClick = {
                            registerLyricsInteraction()
                            showSettings = !showSettings
                        },
                        enableWordByWord = enableWordByWord,
                        onWordByWordChange = {
                            registerLyricsInteraction()
                            enableWordByWord = it
                        }
                    )
                }
            }
        }
    }

    if (showSettings) {
        LyricsSettingsBottomSheet(
            title = "歌词调节",
            subtitle = "把封面页保持简洁，所有歌词动画和逐字参数集中到这里调整。",
            isPhone = true,
            sections = listOf(
                SliderSettingSection(
                    title = "基础节奏",
                    description = "控制歌词滚动、缩放和整体排版密度。",
                    items = listOf(
                        SliderSettingItem("滚动速度", "调整歌词追随播放进度的纵向移动速度。", verticalScrollSpeed, { verticalScrollSpeed = it }, 0.1f..1.0f, 8),
                        SliderSettingItem("缩放速度", "控制当前行进入焦点时的缩放变化速度。", scaleAnimationSpeed, { scaleAnimationSpeed = it }, 0.1f..1.0f, 8),
                        SliderSettingItem("居中放大", "调整当前歌词在视觉中心区域的放大量。", activeLyricSizeRatio, { activeLyricSizeRatio = it }, 0.1f..1.0f, 8),
                        SliderSettingItem("所有字号", "统一放大或缩小整页歌词字号。", baseFontSizeRatio, { baseFontSizeRatio = it }, 0.5f..2.0f, 15),
                        SliderSettingItem("歌词行距", "控制整段歌词上下呼吸感和密度。", lineSpacingRatio, { lineSpacingRatio = it }, 0.5f..3.0f, 25)
                    )
                ),
                SliderSettingSection(
                    title = "逐字动画",
                    description = "逐字歌词开启时，微调上浮、时间偏移和字级缩放。",
                    items = listOf(
                        SliderSettingItem("上浮速度", "控制逐字高亮时的上浮响应速度。", yrcFloatSpeed, { yrcFloatSpeed = it }, 0.1f..2.0f, 18),
                        SliderSettingItem("上浮位移", "控制逐字高亮时向上浮动的距离。", yrcFloatIntensity, { yrcFloatIntensity = it }, 0f..50f, 0),
                        SliderSettingItem("逐字偏移", "整体前移或后移逐字时间点，用于校准听感。", wordTimingOffsetMs, { wordTimingOffsetMs = it }, -1000f..1000f, 39),
                        SliderSettingItem("缩放速度", "控制单字放大的追随速度。", wordScaleSpeed, { wordScaleSpeed = it }, 0.1f..2.0f, 10),
                        SliderSettingItem("缩放大小", "控制单字高亮时的最大放大量。", wordScaleSize, { wordScaleSize = it }, 1.0f..2.0f, 13)
                    )
                )
            ),
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun PhoneAlbumPage(song: Song) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val coverSize = minOf(maxWidth * 0.88f, maxHeight * 0.6f).coerceAtLeast(250.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Spacer(modifier = Modifier.weight(0.18f))

            Box(
                modifier = Modifier
                    .size(coverSize)
                    .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(22.dp),
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(22.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    NeteaseRed.copy(alpha = 0.22f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(22.dp))
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
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = NeteaseRed,
                                modifier = Modifier.size(88.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AudioTrack    FLAC 16 bits    48 kHz",
                color = TextTertiary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.7f))
        }
    }
}
