package com.example.bna.ui.screen.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.example.bna.R
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.player.MusicPlayer
import com.example.bna.player.PlaybackMode
import com.example.bna.player.PlayerState
import com.example.bna.ui.theme.DarkCard
import com.example.bna.ui.theme.NeteaseRed
import com.example.bna.ui.theme.TextPrimary
import com.example.bna.ui.theme.TextSecondary
import com.example.bna.ui.theme.TextTertiary
import kotlin.math.abs

@Composable
fun ProgressBarOnly(
    offsetX: Float = 0f,
    offsetY: Float = 0f,
    widthRatio: Float = 1.0f
) {
    val playerState by MusicPlayer.playerState.collectAsState()
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableStateOf(0f) }
    var pendingSeekPositionMs by remember { mutableStateOf<Long?>(null) }
    val rawProgress = if (playerState.duration > 0) {
        (playerState.currentPosition.toFloat() / playerState.duration).coerceIn(0f, 1f)
    } else 0f
    val targetProgress = when {
        isDragging -> dragProgress
        pendingSeekPositionMs != null && playerState.duration > 0 -> {
            (pendingSeekPositionMs!!.toFloat() / playerState.duration).coerceIn(0f, 1f)
        }
        else -> rawProgress
    }

    val progress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 100, easing = LinearEasing),
        label = "progress"
    )

    LaunchedEffect(playerState.currentPosition, pendingSeekPositionMs) {
        val pending = pendingSeekPositionMs ?: return@LaunchedEffect
        if (abs(playerState.currentPosition - pending) <= 300L) {
            pendingSeekPositionMs = null
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX.dp, y = offsetY.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(widthRatio)) {
            CustomProgressBar(
                value = if (isDragging) dragProgress else progress,
                onValueChange = {
                    pendingSeekPositionMs = null
                    isDragging = true
                    dragProgress = it
                },
                onValueChangeFinished = {
                    val targetMs = (dragProgress * playerState.duration).toLong()
                    pendingSeekPositionMs = targetMs
                    isDragging = false
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
            } else if (pendingSeekPositionMs != null) {
                pendingSeekPositionMs!!
            } else {
                playerState.currentPosition
            }
            Text(text = formatTime(currentDisplayMs), color = TextTertiary, fontSize = 12.sp)
            Text(text = formatTime(playerState.duration), color = TextTertiary, fontSize = 12.sp)
        }
    }
}

@Composable
fun CustomProgressBar(
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
fun PlaybackButtonsOnly(
    isPhone: Boolean = false,
    scale: Float = 1f,
    buttonSizeRatio: Float = 1f,
    buttonSpacingDp: Float = 0f
) {
    val playerState by MusicPlayer.playerState.collectAsState()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 宽度不足时先压缩间距、再整体缩小按钮，保证“上一首/下一首”始终完整显示
        val minSpacing = 8.dp
        val baseSpacing = (if (buttonSpacingDp > 0f) buttonSpacingDp.dp else (if (isPhone) 32.dp else 40.dp) * scale)
            .coerceAtLeast(minSpacing)
        val baseSide = (if (isPhone) 40.dp else 48.dp) * scale * buttonSizeRatio
        val basePlay = (if (isPhone) 64.dp else 72.dp) * scale * buttonSizeRatio
        val fitScale = if (baseSide * 2 + basePlay + minSpacing * 2 > maxWidth) {
            ((maxWidth - minSpacing * 2) / (baseSide * 2 + basePlay)).coerceIn(0.5f, 1f)
        } else 1f
        val sideSize = baseSide * fitScale
        val playSize = basePlay * fitScale
        val sideIcon = (if (isPhone) 28.dp else 32.dp) * scale * buttonSizeRatio * fitScale
        val playIcon = (if (isPhone) 48.dp else 56.dp) * scale * buttonSizeRatio * fitScale
        val spacing = ((maxWidth - sideSize * 2 - playSize) / 2).coerceIn(minSpacing, baseSpacing)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing, Alignment.CenterHorizontally)
        ) {
            IconButton(onClick = { MusicPlayer.playPrevious() }, modifier = Modifier.size(sideSize)) {
                Icon(
                    painter = painterResource(R.drawable.salt_ic_skip_previous),
                    contentDescription = "上一首",
                    tint = TextPrimary,
                    modifier = Modifier.size(sideIcon)
                )
            }

            IconButton(onClick = { MusicPlayer.togglePlayPause() }, modifier = Modifier.size(playSize)) {
                Icon(
                    painter = painterResource(
                        if (playerState.isPlaying) R.drawable.salt_ic_pause_btn else R.drawable.salt_ic_play_btn
                    ),
                    contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                    tint = Color.White,
                    modifier = Modifier.size(playIcon)
                )
            }

            IconButton(onClick = { MusicPlayer.playNext() }, modifier = Modifier.size(sideSize)) {
                Icon(
                    painter = painterResource(R.drawable.salt_ic_skip_next),
                    contentDescription = "下一首",
                    tint = Color.White,
                    modifier = Modifier.size(sideIcon)
                )
            }
        }
    }
}

@Composable
fun PlaybackControls(
    isPhone: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        ProgressBarOnly()
        Spacer(modifier = Modifier.height(if (isPhone) 20.dp else 24.dp))
        PlaybackButtonsOnly(isPhone = isPhone)
    }
}

@Composable
fun PlaylistOverlayPanel(
    playerState: PlayerState,
    isPhone: Boolean,
    onClose: () -> Unit,
    // 「跟手」回调：onDownDrag(deltaPx) delta>0=手指下移(关闭面板)、delta<0=手指上移(重新打开)。
    // 宿主统一按 openPx = (openPx - delta).coerceIn(0..maxOpenPx) 处理即可双向跟手。
    onDownDrag: (Float) -> Unit = {},
    // 关闭手势松手后调用：由宿主吸附(settle)
    onDownDragEnd: () -> Unit = {}
) {
    val playlist = playerState.playlist
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // 本次「按住」期间是否已通过“歌单滚到顶再下拉”把面板往下拖过。
    // 用于在真正松手时才吸附，避免按住不动时仍被 140ms 定时器自动吸附。
    var pulledDownThisPress by remember { mutableStateOf(false) }

    // 歌单滚到最顶后再下拉 → 交给面板关闭(嵌套滚动)。
    // 吸附(settle)不再用固定延时近似松手，而是等手指真正抬起时统一触发(见下方 pointerInput 监测)。
    val closeNested = remember(listState) {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
            ): androidx.compose.ui.geometry.Offset {
                val atTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput &&
                    available.y > 0f && atTop
                ) {
                    pulledDownThisPress = true
                    onDownDrag(available.y)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // 手机/平板：面板高度都占满所在区域，歌单用 weight 填满到面板底，显示更多歌；
            // navigationBarsPadding 让底部避开系统导航栏，末行歌曲不再被遮挡/截断。
            .fillMaxHeight()
            .navigationBarsPadding()
            .padding(horizontal = if (isPhone) 12.dp else 28.dp)
            .padding(bottom = if (isPhone) 20.dp else 32.dp)
            .nestedScroll(closeNested)
            // 只监测不消费：面板内任意位置抬起手指时，若本次确有“歌单滚到顶再下拉”拖过面板，
            // 才触发吸附(settle)。这样手指按住不动时不会自动吸附，跟手停留。
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var wasPressed = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressedNow = event.changes.any { it.pressed }
                        if (pressedNow && !wasPressed) {
                            pulledDownThisPress = false
                        } else if (!pressedNow && wasPressed) {
                            if (pulledDownThisPress) {
                                pulledDownThisPress = false
                                onDownDragEnd()
                            }
                        }
                        wasPressed = pressedNow
                    }
                }
            }
    ) {
        val modeIcon = when (playerState.playbackMode) {
            PlaybackMode.LIST_LOOP -> Icons.Default.Repeat
            PlaybackMode.SINGLE_LOOP -> Icons.Default.RepeatOne
            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
        }
        val modeLabel = when (playerState.playbackMode) {
            PlaybackMode.LIST_LOOP -> "列表循环"
            PlaybackMode.SINGLE_LOOP -> "单曲循环"
            PlaybackMode.SHUFFLE -> "随机播放"
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // 顶部整块区域下拖即关闭(跟手)；下拖途中反向上拖可再打开，双向跟手往返
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (dragAmount != 0f) {
                                change.consume()
                                onDownDrag(dragAmount)
                            }
                        },
                        onDragEnd = { onDownDragEnd() },
                        onDragCancel = { onDownDragEnd() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 单曲循环/随机播放/列表循环 移到播放列表内：单图标循环点击切换
            IconButton(
                onClick = { MusicPlayer.cyclePlaybackMode() },
                modifier = Modifier.size(if (isPhone) 44.dp else 52.dp)
            ) {
                Icon(
                    imageVector = modeIcon,
                    contentDescription = modeLabel,
                    tint = NeteaseRed,
                    modifier = Modifier.size(if (isPhone) 24.dp else 28.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前播放",
                    color = TextPrimary,
                    fontSize = if (isPhone) 18.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "共 ${playlist.size} 首 · $modeLabel",
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onClose, modifier = Modifier.size(if (isPhone) 40.dp else 48.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = TextSecondary,
                    modifier = Modifier.size(if (isPhone) 22.dp else 26.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (playlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "播放列表为空", color = TextTertiary, fontSize = 14.sp)
            }
            return
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                // 手机/平板统一：填满 header 下方余下空间到面板底部（weight 由外层 Column 提供有界高度），
                // 不再限死 420dp，可滚动且底部不再被截断。
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            itemsIndexed(playlist, key = { _, song -> song.id }) { index, song ->
                val isCurrent = index == playerState.currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (isCurrent) NeteaseRed.copy(alpha = 0.16f) else DarkCard.copy(alpha = 0.55f)
                        )
                        .clickable {
                            // 切换歌曲后保持在播放列表，不自动关闭
                            MusicPlayer.playSongAt(index)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "当前播放",
                                tint = NeteaseRed,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(text = "${index + 1}", color = TextTertiary, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(if (isPhone) 42.dp else 46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkCard.copy(alpha = 0.9f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.albumCoverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = song.albumCoverUrl + "?param=120y120",
                                contentDescription = song.albumName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.name,
                            // 所有歌曲标题统一纯白
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artistNames,
                            color = TextTertiary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }

                    if (isCurrent) {
                        Text(
                            text = "正在播放",
                            color = NeteaseRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
