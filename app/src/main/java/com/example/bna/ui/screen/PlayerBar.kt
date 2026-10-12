package com.example.bna.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import com.example.bna.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.player.MusicPlayer
import com.example.bna.player.PlayerState
import com.example.bna.ui.animation.SaltMotion
import com.example.bna.ui.theme.*

/** 悬浮圆角迷你播放器：封面取色、平滑进度与播放按钮过渡。 */
@Composable
fun PlayerBar(onClick: () -> Unit) {
    val playerState by MusicPlayer.playerState.collectAsState()

    AnimatedVisibility(
        visible = playerState.currentSong != null,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = SaltMotion.FastSpatialOffset
        ) + fadeIn(animationSpec = SaltMotion.DefaultEffects),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = SaltMotion.FastSpatialOffset
        ) + fadeOut(animationSpec = SaltMotion.FastEffects)
    ) {
        PlayerBarContent(state = playerState, onClick = onClick)
    }
}

/** 浮动卡片留出封面上下各 12dp。 */
private val MiniBarHeight = 72.dp

/** salt `u41.java:267`：播放/暂停按钮 40dp。 */
private val MiniPlayButtonSize = 40.dp

/** 圆角方形封面，也是共享封面过渡的起点。 */
private val MiniCoverSize = 48.dp

@Composable
private fun PlayerBarContent(state: PlayerState, onClick: () -> Unit) {
    val song = state.currentSong ?: return
    val colors = LocalHomeColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        colors.surface,
                        androidx.compose.ui.graphics.lerp(colors.surface, colors.accent, 0.09f)
                    )
                )
            )
    ) {
        // 顶部分割线
        HorizontalDivider(
            modifier = Modifier.align(Alignment.TopStart),
            thickness = 0.5.dp,
            color = DividerColor
        )

        // 背景进度条：用 animateFloatAsState 平滑推进，避免每帧硬跳
        if (state.duration > 0) {
            val targetProgress =
                (state.currentPosition.toFloat() / state.duration.toFloat()).coerceIn(0f, 1f)
            val progress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(durationMillis = 300, easing = LinearEasing),
                label = "miniProgress"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(3.dp)
                    .align(Alignment.BottomStart)
                    .background(colors.accent.copy(alpha = 0.65f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniBarHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 圆角封面 ──
            MiniCover(song = song)

            Spacer(modifier = Modifier.width(12.dp))

            // ── 歌曲信息 ──
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.name,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = song.artistNames,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── 播放/暂停：salt 用 AnimatedContent 做图标形变 ──
            MiniPlayPauseButton(
                isPlaying = state.isPlaying,
                isLoading = state.isLoading
            )

            Spacer(modifier = Modifier.width(4.dp))

            // ── 关闭/清空队列 ──
            IconButton(
                onClick = { MusicPlayer.clearQueue() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = TextTertiary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** 静止的圆角封面，与全屏封面自然衔接。 */
@Composable
private fun MiniCover(song: com.example.bna.data.model.Song) {
    val flight = LocalCoverFlight.current
    Box(
        modifier = Modifier
            .size(MiniCoverSize)
            .flightCover(expanded = false)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard),
        contentAlignment = Alignment.Center
    ) {
        if (song.albumCoverUrl.isNotEmpty()) {
            AsyncImage(
                model = song.albumCoverUrl + "?param=80y80",
                onSuccess = { flight?.artwork = it.painter },
                contentDescription = song.albumName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = LocalHomeColors.current.accent,
                modifier = Modifier.size(20.dp)
            )
        }

    }
}

/**
 * 播放 / 暂停按钮。
 *
 * 对齐 salt `u41.m6340`：状态切换用 `AnimatedContent` 做**图标形变**
 * （淡入淡出 + 缩放），而不是硬切图标。
 */
@Composable
private fun MiniPlayPauseButton(isPlaying: Boolean, isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier.size(MiniPlayButtonSize),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = LocalHomeColors.current.accent,
                strokeWidth = 2.dp
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .size(MiniPlayButtonSize)
            .clip(RoundedCornerShape(50))
            .background(LocalHomeColors.current.accent.copy(alpha = 0.15f))
            .clickable { MusicPlayer.togglePlayPause() },
        contentAlignment = Alignment.Center
    ) {
        // salt `u41.java:291`：`$this$AnimatedContent`
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                // 图标形变：淡入淡出 + 缩放（新图标放大入场，旧图标缩小退场）
                (fadeIn(animationSpec = SaltMotion.FastEffects) +
                    scaleIn(
                        initialScale = 0.6f,
                        animationSpec = SaltMotion.FastSpatial
                    ))
                    .togetherWith(
                        fadeOut(animationSpec = SaltMotion.FastEffects) +
                            scaleOut(
                                targetScale = 0.6f,
                                animationSpec = SaltMotion.FastSpatial
                            )
                    )
            },
            label = "AnimatedContent"
        ) { playing ->
            Icon(
                painter = painterResource(
                    if (playing) R.drawable.salt_ic_pause_btn else R.drawable.salt_ic_play_btn
                ),
                contentDescription = if (playing) "暂停" else "播放",
                tint = LocalHomeColors.current.accent,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
