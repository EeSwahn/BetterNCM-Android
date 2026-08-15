package com.example.bna.ui.screen.lyrics

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
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
    var glowBrightness by rememberFloatPreference("glowBrightness", 0.09f)
    var glowBreathFrequency by rememberFloatPreference("glowBreathFrequency", 0.5f)
    var glowScaleSize by rememberFloatPreference("glowScaleSize", 1.3f)
    var enableEdgeGlow by rememberBooleanPreference("enableEdgeGlow", true)
    var rightEdgeGlowRadius by rememberFloatPreference("rightEdgeGlowRadius", 98.0f)
    var beatGlowThreshold by rememberFloatPreference("beatGlowThreshold", 0.1f)
    var beatGlowDelayMs by rememberFloatPreference("beatGlowDelayMs", 352.9f)
    var showSettings by remember { mutableStateOf(false) }
    var lyricsControlsVisible by remember { mutableStateOf(true) }
    var lyricsInteractionVersion by remember { mutableStateOf(0) }

    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF00BFFF)) }
    LaunchedEffect(song.albumCoverUrl) {
        if (song.albumCoverUrl.isNotEmpty()) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(song.albumCoverUrl + "?param=200y200")
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        dominantColor = extractVibrantColor(bitmap)
                    }
                }
            } catch (_: Exception) { }
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        var audioAmplitude by remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            val history = mutableListOf<Pair<Long, Float>>()
            while (true) {
                val now = System.currentTimeMillis()
                val currentAmp = com.example.bna.player.MusicPlayer.suiXinChangProcessor.currentAmplitude
                history.add(now to currentAmp)
                history.removeAll { now - it.first > 2000 } // Keep up to 2 seconds of history
                
                val targetTime = now - beatGlowDelayMs.toLong()
                val delayedAmp = history.minByOrNull { Math.abs(it.first - targetTime) }?.second ?: currentAmp
                
                audioAmplitude = delayedAmp
                kotlinx.coroutines.delay(16) // use 16ms for smooth 60fps polling
            }
        }
        val targetEdgeAlpha = if (audioAmplitude > beatGlowThreshold) 0.2f + audioAmplitude * 0.8f else 0f
        val animatedEdgeAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = targetEdgeAlpha,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "edgeAlphaAnim"
        )
        if (enableEdgeGlow && rightEdgeGlowRadius > 0f) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(animatedEdgeAlpha)
                    .blur(30.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            ) {
                val w = size.width
                val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w, 0f)
                    quadraticTo(w - 2 * rightEdgeGlowRadius, h / 2f, w, h)
                    close()
                }
                drawPath(
                    path = path,
                    color = dominantColor.copy(alpha = 0.5f)
                )
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
                        ScanningGlowText(
                            text = song.name,
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            durationMillis = 5500,
                            glowColor = Color.White
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
                        ScanningGlowText(
                            text = song.name,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            durationMillis = 5500,
                            glowColor = Color.White
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
                PhoneAlbumPage(
                    song = song,
                    glowBrightness = glowBrightness,
                    glowBreathFrequency = glowBreathFrequency,
                    glowScaleSize = glowScaleSize,
                    dominantColor = dominantColor,
                    audioAmplitude = audioAmplitude
                )
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
                        }
                    )
                }
            }
        }
    }
    }

    if (showSettings) {
        LyricsSettingsBottomSheet(
            title = "歌词调节",
            subtitle = "把封面页保持简洁，所有歌词动画和逐字参数集中到这里调整。",
            isPhone = true,
            wordByWordEnabled = enableWordByWord,
            onWordByWordChange = {
                registerLyricsInteraction()
                enableWordByWord = it
            },
            sections = listOf(
                SliderSettingSection(
                    title = "封面发光",
                    description = "封面周围的发光效果，颜色自动从封面主色调提取。",
                    items = listOf(
                        SliderSettingItem("发光亮度", "控制封面光晕的整体亮度，为 0 时关闭发光。", glowBrightness, { glowBrightness = it }, 0f..1.0f, 10),
                        SliderSettingItem("发光大小", "控制封面下方发光图层的缩放比例。", glowScaleSize, { glowScaleSize = it }, 1.0f..3.0f, 20),
                        SliderSettingItem("呼吸频率", "发光明暗交替的速度，为 0 时光晕保持静态。", glowBreathFrequency, { glowBreathFrequency = it }, 0f..5.0f, 10)
                    )
                ),
                SliderSettingSection(
                    title = "边缘发光",
                    description = "屏幕边缘的发光效果。",
                    items = listOf(
                        SwitchSettingItem("启用发光", "开启或关闭屏幕边缘的随低音发光效果。", enableEdgeGlow, { enableEdgeGlow = it }),
                        SliderSettingItem("右边缘发光", "控制右侧边缘发光的半径。无论怎么调整，发光始终经过右侧上下两点。", rightEdgeGlowRadius, { rightEdgeGlowRadius = it }, 0f..500f, 50),
                        SliderSettingItem("发光鼓点阈值", "过滤微弱振幅，仅当低音强度高于此值时发光。", beatGlowThreshold, { beatGlowThreshold = it }, 0f..1.0f, 50),
                        SliderSettingItem("发光延迟补偿", "如果发光比声音早，可增加此延迟让光晕踩准鼓点。", beatGlowDelayMs, { beatGlowDelayMs = it }, 0f..1000f, 50)
                    )
                ),
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
private fun PhoneAlbumPage(
    song: Song,
    glowBrightness: Float,
    glowBreathFrequency: Float,
    glowScaleSize: Float,
    dominantColor: Color,
    audioAmplitude: Float
) {
    val context = LocalContext.current
    val breathAnim = remember { androidx.compose.animation.core.Animatable(1.0f) }
    LaunchedEffect(glowBreathFrequency) {
        if (glowBreathFrequency > 0.01f) {
            val periodMs = (1000f / glowBreathFrequency).toInt().coerceAtLeast(200)
            while (true) {
                breathAnim.animateTo(
                    targetValue = 0.45f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = periodMs)
                )
                breathAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = periodMs)
                )
            }
        } else {
            breathAnim.snapTo(1.0f)
        }
    }
    val effectiveGlowAlpha = glowBrightness * breathAnim.value

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
                contentAlignment = Alignment.Center
            ) {
                // 发光层：位于封面后面，使用低分辨率缩略图做高斯模糊，避免模糊原图消耗性能
                if (glowBrightness > 0.01f && song.albumCoverUrl.isNotEmpty()) {
                    val colorMatrix = remember { ColorMatrix().apply { setToSaturation(1.4f) } }
                    AsyncImage(
                        model = song.albumCoverUrl + "?param=32y32",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.colorMatrix(colorMatrix),
                        modifier = Modifier
                            .requiredSize(coverSize * glowScaleSize)
                            .clip(RoundedCornerShape(22.dp * glowScaleSize))
                            .graphicsLayer { 
                                alpha = 0.6f * breathAnim.value 
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                val fadeFraction = 0.15f
                                drawRect(
                                    brush = Brush.horizontalGradient(
                                        0.0f to Color.Transparent,
                                        fadeFraction to Color.Black,
                                        1f - fadeFraction to Color.Black,
                                        1.0f to Color.Transparent,
                                        startX = 0f,
                                        endX = size.width
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        0.0f to Color.Transparent,
                                        fadeFraction to Color.Black,
                                        1f - fadeFraction to Color.Black,
                                        1.0f to Color.Transparent,
                                        startY = 0f,
                                        endY = size.height
                                    ),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                            .blur(48.dp, BlurredEdgeTreatment.Unbounded)
                    )
                }

                // 封面本体
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
