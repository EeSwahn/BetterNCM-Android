package com.example.bna.ui.screen.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.player.MusicPlayer
import com.example.bna.player.PlaybackMode
import com.example.bna.player.PlayerState
import com.example.bna.ui.theme.DarkBackground
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
    isPhone: Boolean = false
) {
    val playerState by MusicPlayer.playerState.collectAsState()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isPhone) 32.dp else 40.dp, Alignment.CenterHorizontally)
    ) {
        IconButton(onClick = { MusicPlayer.playPrevious() }, modifier = Modifier.size(48.dp)) {
            Icon(
                imageVector = Icons.Default.SkipPrevious,
                contentDescription = "上一首",
                tint = TextPrimary,
                modifier = Modifier.size(if (isPhone) 28.dp else 32.dp)
            )
        }

        IconButton(onClick = { MusicPlayer.togglePlayPause() }, modifier = Modifier.size(72.dp)) {
            Icon(
                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (playerState.isPlaying) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(if (isPhone) 48.dp else 56.dp)
            )
        }

        IconButton(onClick = { MusicPlayer.playNext() }, modifier = Modifier.size(48.dp)) {
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
@OptIn(ExperimentalMaterial3Api::class)
fun BottomActionButtons(
    isPhone: Boolean = false,
    modifier: Modifier = Modifier,
    enableWordByWord: Boolean = false,
    onWordByWordChange: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit
) {
    val playerState by MusicPlayer.playerState.collectAsState()
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlaylistSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isPhone) 8.dp else 0.dp)
            .padding(bottom = if (isPhone) 0.dp else 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val playbackModeIcon = when (playerState.playbackMode) {
            PlaybackMode.LIST_LOOP -> Icons.Default.Repeat
            PlaybackMode.SINGLE_LOOP -> Icons.Default.RepeatOne
            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle
        }
        val playbackModeLabel = when (playerState.playbackMode) {
            PlaybackMode.LIST_LOOP -> "列表循环"
            PlaybackMode.SINGLE_LOOP -> "单曲循环"
            PlaybackMode.SHUFFLE -> "随机播放"
        }

        ActionButton(
            icon = playbackModeIcon,
            contentDescription = playbackModeLabel,
            isPhone = isPhone,
            onClick = { MusicPlayer.cyclePlaybackMode() },
            tint = NeteaseRed
        )
        ActionButton(Icons.Default.Timer, "定时关闭", isPhone)
        ActionButton(Icons.Default.Tune, "设置", isPhone, onClick = onSettingsClick)
        ActionButton(Icons.Default.List, "播放列表", isPhone, onClick = { showPlaylistSheet = true })

        Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
            ActionButton(Icons.Default.MoreHoriz, "更多", isPhone, onClick = { showMoreMenu = true })

            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false },
                modifier = Modifier.background(DarkCard)
            ) {
                DropdownMenuItem(
                    text = { Text("更多选项", color = TextPrimary) },
                    onClick = { showMoreMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("逐字歌词", color = TextPrimary) },
                    onClick = {
                        onWordByWordChange(!enableWordByWord)
                        showMoreMenu = false
                    },
                    trailingIcon = {
                        Switch(
                            checked = enableWordByWord,
                            onCheckedChange = null,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeteaseRed,
                                checkedTrackColor = NeteaseRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                )
            }
        }
    }

    if (showPlaylistSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlaylistSheet = false },
            containerColor = DarkBackground,
            contentColor = TextPrimary
        ) {
            PlaylistBottomSheetContent(
                playerState = playerState,
                isPhone = isPhone,
                onDismiss = { showPlaylistSheet = false }
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    contentDescription: String,
    isPhone: Boolean,
    onClick: () -> Unit = {},
    tint: Color = TextSecondary
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (isPhone) 40.dp else 48.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(if (isPhone) 24.dp else 28.dp)
        )
    }
}

@Composable
private fun PlaylistBottomSheetContent(
    playerState: PlayerState,
    isPhone: Boolean,
    onDismiss: () -> Unit
) {
    val playlist = playerState.playlist

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "当前播放",
                    color = TextPrimary,
                    fontSize = if (isPhone) 18.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "共 ${playlist.size} 首", color = TextTertiary, fontSize = 12.sp)
            }
            TextButton(onClick = onDismiss) {
                Text(text = "关闭", color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isPhone) 420.dp else 520.dp),
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
                            if (isCurrent) NeteaseRed.copy(alpha = 0.14f) else DarkCard.copy(alpha = 0.62f)
                        )
                        .clickable {
                            MusicPlayer.playSongAt(index)
                            onDismiss()
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
                            .background(DarkCard),
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
                            color = if (isCurrent) TextPrimary else TextSecondary,
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
