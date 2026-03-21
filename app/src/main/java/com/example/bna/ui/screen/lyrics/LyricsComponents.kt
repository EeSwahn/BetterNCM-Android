package com.example.bna.ui.screen.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bna.data.model.LyricLine
import com.example.bna.player.MusicPlayer
import com.example.bna.ui.animation.LyricsAnimationConfig
import com.example.bna.ui.animation.flowingLightEffect
import com.example.bna.ui.animation.glowEffect
import com.example.bna.ui.animation.lyrics3DEffect
import com.example.bna.ui.animation.rememberLyricsAnimationConfig
import com.example.bna.ui.theme.NeteaseRed
import com.example.bna.ui.theme.TextSecondary
import com.example.bna.ui.theme.TextTertiary
import com.example.bna.viewmodel.LyricsUiState
import com.example.bna.viewmodel.LyricsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.max

@Composable
fun LyricsPanel(
    lyricsState: LyricsUiState,
    lyricsViewModel: LyricsViewModel,
    onDismiss: () -> Unit,
    isPhone: Boolean = false,
    animationConfig: LyricsAnimationConfig = rememberLyricsAnimationConfig(),
    verticalScrollSpeed: Float = 1.0f,
    scaleAnimationSpeed: Float = 1.0f,
    activeLyricSizeRatio: Float = 1.0f,
    baseFontSizeRatio: Float = 1.0f,
    lineSpacingRatio: Float = 0.7f,
    enableWordByWord: Boolean = false,
    yrcFloatSpeed: Float = 1.0f,
    yrcFloatIntensity: Float = 12f,
    wordTimingOffsetMs: Float = 0f,
    wordScaleSpeed: Float = 1.0f,
    wordScaleSize: Float = 1.3f
) {
    val listState = rememberLazyListState()

    LaunchedEffect(lyricsState.lyrics.firstOrNull()?.timeMs, lyricsState.lyrics.size) {
        if (lyricsState.lyrics.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(lyricsState.currentLineIndex) {
        val activeIndex = lyricsState.currentLineIndex
        if (activeIndex < 0) {
            listState.scrollToItem(0)
            return@LaunchedEffect
        }

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
            val activeCenter = activeItemInfo.offset + (activeItemInfo.size / 2)
            val viewportCenter = (viewportHeight * if (isPhone) 0.34f else 0.38f).toInt()
            val distanceToScroll = activeCenter - viewportCenter
            val duration = (500 / verticalScrollSpeed).toInt().coerceAtLeast(100)
            listState.animateScrollBy(
                value = distanceToScroll.toFloat(),
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            )
        }
    }

    when {
        lyricsState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeteaseRed)
            }
        }
        lyricsState.hasNoLyric || lyricsState.lyrics.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "暂无歌词", color = TextTertiary, fontSize = 16.sp)
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
                    val distance = kotlin.math.abs(index - lyricsState.currentLineIndex)
                    LyricLineItem(
                        line = line,
                        isCurrent = index == lyricsState.currentLineIndex,
                        distance = distance,
                        scaleAnimationSpeed = scaleAnimationSpeed,
                        activeLyricSizeRatio = activeLyricSizeRatio,
                        baseFontSizeRatio = baseFontSizeRatio,
                        lineSpacingRatio = lineSpacingRatio,
                        onClick = { MusicPlayer.seekTo(line.timeMs) },
                        isPhone = isPhone,
                        enableWordByWord = enableWordByWord,
                        yrcFloatSpeed = yrcFloatSpeed,
                        yrcFloatIntensity = yrcFloatIntensity,
                        wordTimingOffsetMs = wordTimingOffsetMs,
                        wordScaleSpeed = wordScaleSpeed,
                        wordScaleSize = wordScaleSize
                    )
                }
            }
        }
    }
}

@Composable
fun LyricLineItem(
    line: LyricLine,
    isCurrent: Boolean,
    distance: Int,
    scaleAnimationSpeed: Float = 1.0f,
    activeLyricSizeRatio: Float = 1.0f,
    baseFontSizeRatio: Float = 1.0f,
    lineSpacingRatio: Float = 0.7f,
    onClick: () -> Unit,
    isPhone: Boolean = false,
    enableWordByWord: Boolean = false,
    yrcFloatSpeed: Float = 1.0f,
    yrcFloatIntensity: Float = 12f,
    wordTimingOffsetMs: Float = 0f,
    wordScaleSpeed: Float = 1.0f,
    wordScaleSize: Float = 1.3f
) {
    val isPlaying by remember {
        MusicPlayer.playerState.map { it.isPlaying }.distinctUntilChanged()
    }.collectAsState(initial = MusicPlayer.playerState.value.isPlaying)
    val playerState by MusicPlayer.playerState.collectAsState()
    val animationConfig = rememberLyricsAnimationConfig()

    val animationDuration = (400 / scaleAnimationSpeed).toInt().coerceAtLeast(100)
    val baseActiveFontSize = if (isPhone) 26f else 44f
    val baseActiveLineHeight = if (isPhone) 36f else 60f
    val inactiveScale = if (isPhone) 16f / 26f else 26f / 44f
    val targetScale = if (isCurrent) activeLyricSizeRatio else inactiveScale

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "scale"
    )

    val fontSize = (baseFontSizeRatio * baseActiveFontSize).coerceIn(8f, 72f)
    val lineHeight = (baseFontSizeRatio * baseActiveLineHeight).coerceIn(12f, 96f)
    val wordsToUse by remember(line.text, line.durationMs, line.words) {
        mutableStateOf(line.words ?: generateWordInfoForLine(line))
    }
    val usesWordByWordFlow = enableWordByWord && wordsToUse.isNotEmpty() && wordsToUse.size <= 50

    val targetAlpha = if (isCurrent) 1f else 0.3f
    val animatedLineAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    val lineAlpha = if (usesWordByWordFlow) 1f else animatedLineAlpha

    val textColor by animateColorAsState(
        targetValue = if (isCurrent) Color.White else TextSecondary,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing),
        label = "textColor"
    )
    val effectiveCurrentPosition = playerState.currentPosition + wordTimingOffsetMs.toLong()

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
                if (animationConfig.enable3DEffect) modifier.lyrics3DEffect(isCurrent, distance) else modifier
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
                if (animationConfig.enableGlowEffect) modifier.glowEffect(isCurrent = isCurrent, glowColor = Color.White) else modifier
            }
            .padding(
                horizontal = if (isPhone) 4.dp else 32.dp,
                vertical = 4.dp * lineSpacingRatio
            )
            .graphicsLayer {
                alpha = lineAlpha
                scaleX = scale
                scaleY = scale
                transformOrigin = if (isPhone) TransformOrigin(0.5f, 0.5f) else TransformOrigin(0f, 0.5f)
            }
    ) {
        if (enableWordByWord) {
            if (usesWordByWordFlow) {
                val maxFloatOffset = yrcFloatIntensity * 1.5f * yrcFloatSpeed
                val pendingWordAlpha = 0.3f
                WordByWordLyricsFlow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp * lineSpacingRatio)
                        .padding(top = (maxFloatOffset / 2).dp),
                    centerLines = isPhone
                ) {
                    wordsToUse.forEachIndexed { index, word ->
                        val wordStart = line.timeMs + word.startOffset.toLong()
                        val wordEnd = wordStart + word.duration.toLong()
                        val isActive = effectiveCurrentPosition in wordStart until wordEnd
                        val isPassed = effectiveCurrentPosition >= wordEnd
                        val isAnimatedWord = isCurrent && (isActive || isPassed)
                        val isBeforeLine = effectiveCurrentPosition < line.timeMs
                        val targetWordAlpha = when {
                            !isCurrent -> pendingWordAlpha
                            isBeforeLine -> pendingWordAlpha
                            isAnimatedWord -> 1.0f
                            else -> pendingWordAlpha
                        }
                        val targetWordScale = if (isCurrent && !isBeforeLine && isAnimatedWord) wordScaleSize else 1.0f
                        val targetTranslationY = if (isCurrent && !isBeforeLine && isAnimatedWord) -maxFloatOffset else 0f
                        val scaleDuration = (200 / wordScaleSpeed).toInt().coerceAtLeast(50)

                        val wordAlpha by animateFloatAsState(
                            targetValue = targetWordAlpha,
                            animationSpec = tween(
                                durationMillis = (150 / wordScaleSpeed).toInt().coerceAtLeast(50),
                                easing = FastOutSlowInEasing
                            ),
                            label = "wa_${line.timeMs}_${index}"
                        )
                        val wordScale by animateFloatAsState(
                            targetValue = targetWordScale,
                            animationSpec = tween(durationMillis = scaleDuration, easing = FastOutSlowInEasing),
                            label = "ws_${line.timeMs}_${index}"
                        )
                        val wordTranslationY by animateFloatAsState(
                            targetValue = targetTranslationY,
                            animationSpec = tween(durationMillis = scaleDuration, easing = LinearOutSlowInEasing),
                            label = "wt_${line.timeMs}_${index}"
                        )
                        val wordColor by animateColorAsState(
                            targetValue = when {
                                !isCurrent -> TextSecondary
                                isBeforeLine -> TextSecondary
                                isAnimatedWord -> Color.White
                                else -> TextSecondary
                            },
                            animationSpec = tween(
                                durationMillis = (150 / wordScaleSpeed).toInt().coerceAtLeast(50)
                            ),
                            label = "wc_${line.timeMs}_${index}"
                        )

                        Text(
                            text = word.text,
                            color = wordColor,
                            fontSize = fontSize.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = lineHeight.sp,
                            modifier = Modifier
                                .alpha(wordAlpha)
                                .graphicsLayer {
                                    scaleX = wordScale
                                    scaleY = wordScale
                                    translationY = wordTranslationY
                                }
                        )
                    }
                }
            } else if (isCurrent) {
                val progress = getDetailedLyricsProgress(effectiveCurrentPosition, line)
                Text(
                    text = line.text,
                    style = LocalTextStyle.current.copy(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.White,
                            progress to Color.White,
                            progress + 0.001f to Color.White.copy(alpha = 0.35f),
                            1.0f to Color.White.copy(alpha = 0.35f)
                        )
                    ),
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isPhone) TextAlign.Center else TextAlign.Start,
                    lineHeight = lineHeight.sp,
                    modifier = Modifier.padding(vertical = 4.dp * lineSpacingRatio)
                )
            } else {
                Text(
                    text = line.text,
                    color = textColor,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (isPhone) TextAlign.Center else TextAlign.Start,
                    lineHeight = lineHeight.sp,
                    modifier = Modifier.padding(vertical = 4.dp * lineSpacingRatio)
                )
            }
        } else {
            Text(
                text = line.text,
                color = textColor,
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (isPhone) TextAlign.Center else TextAlign.Start,
                lineHeight = lineHeight.sp,
                modifier = Modifier.padding(vertical = 4.dp * lineSpacingRatio)
            )
        }

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
private fun WordByWordLyricsFlow(
    modifier: Modifier = Modifier,
    centerLines: Boolean,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val availableWidth = constraints.maxWidth
        val wrapTolerancePx = if (availableWidth == Constraints.Infinity) 0 else max(6, availableWidth / 80)
        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val rows = mutableListOf<FlowRowData>()

        var currentRowItems = mutableListOf<Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        fun commitRow() {
            if (currentRowItems.isEmpty()) return
            rows += FlowRowData(
                placeables = currentRowItems.toList(),
                width = currentRowWidth,
                height = currentRowHeight
            )
            currentRowItems = mutableListOf()
            currentRowWidth = 0
            currentRowHeight = 0
        }

        placeables.forEach { placeable ->
            val shouldWrap = currentRowItems.isNotEmpty() &&
                availableWidth != Constraints.Infinity &&
                currentRowWidth + placeable.width > availableWidth + wrapTolerancePx

            if (shouldWrap) commitRow()

            currentRowItems += placeable
            currentRowWidth += placeable.width
            currentRowHeight = max(currentRowHeight, placeable.height)
        }
        commitRow()

        val contentWidth = if (availableWidth == Constraints.Infinity) {
            rows.maxOfOrNull { it.width } ?: 0
        } else {
            availableWidth
        }
        val contentHeight = rows.sumOf { it.height }
        val layoutWidth = contentWidth.coerceAtLeast(constraints.minWidth)
        val layoutHeight = when {
            constraints.maxHeight == Constraints.Infinity -> contentHeight.coerceAtLeast(constraints.minHeight)
            else -> contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        }

        layout(width = layoutWidth, height = layoutHeight) {
            var y = 0
            rows.forEach { row ->
                val startX = if (centerLines) ((contentWidth - row.width) / 2).coerceAtLeast(0) else 0
                var x = startX
                row.placeables.forEach { placeable ->
                    placeable.placeRelative(x = x, y = y + ((row.height - placeable.height) / 2))
                    x += placeable.width
                }
                y += row.height
            }
        }
    }
}

private data class FlowRowData(
    val placeables: List<Placeable>,
    val width: Int,
    val height: Int
)
