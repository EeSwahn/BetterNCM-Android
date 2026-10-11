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

/**
 * 底部迷你播放条 —— 对齐椒盐音乐（Salt Player）v11.0.4 的迷你播放器。
 *
 * ## 逆向依据（【观测】，来自反编译源码）
 *
 * | 项 | salt 位置 | 值 |
 * |---|---|---|
 * | 控制条高 | `cy1.java:112` (`float f2 = 56`) | **56dp** |
 * | 播放/暂停按钮 | `u41.java:267` (`dx2.m1628(..., 40)`) | **40dp** |
 * | 上一首/下一首 | `cy1.java:113/174` (`dx2.m1624(..., 56)`) | 56dp 触摸区 |
 * | 队列按钮 | `u41.java:130` (`dx2.m1624(..., 48)`) | 48dp |
 * | 最小触摸目标 | `u41.java:163` (`dx2.m1619(..., 128)`) | **128dp** |
 * | 播放/暂停图标 | `u41.java:260/264` | 专用 `ic_mini_player_play` / `ic_mini_player_pause` |
 * | 图标切换方式 | `u41.java:291` 出现 `"$this$AnimatedContent"` | **`AnimatedContent` 形变** |
 *
 * `u41.m6340` 是播放/暂停按钮本体：它用 `AbstractC0372.m8278`（= 一个 `AnimatedContent` 包装，
 * 默认标签 `"AnimatedContent"`，已用 REA 恢复，evidence
 * `ev_ca7c81970e1749254abfdfc0f54cc25cb7ce6773ce111ff0e2f939d726be1f8d`）
 * 在两种状态间做过渡。
 *
 * ## 本项目的等效实现
 * - 播放/暂停图标 → `AnimatedContent` + fade+scale（对齐 salt）；
 * - 封面 → 播放时**匀速旋转**（本项目此前注释写"旋转效果"但实际未实现）；
 * - 进度条 → `animateFloatAsState` 平滑推进（此前是每帧硬跳）；
 * - 进/出场 → `AnimatedVisibility` + salt 的"空间/效果"曲线。
 */
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

/** salt `cy1.java:112`：控制条高 56dp。 */
private val MiniBarHeight = 56.dp

/** salt `u41.java:267`：播放/暂停按钮 40dp。 */
private val MiniPlayButtonSize = 40.dp

/** 封面尺寸：沿用 salt 迷你条的紧凑比例（条高 56dp 内留出上下呼吸）。 */
private val MiniCoverSize = 40.dp

@Composable
private fun PlayerBarContent(state: PlayerState, onClick: () -> Unit) {
    val song = state.currentSong ?: return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1A0510),
                        Color(0xFF1A1A1A)
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
                    .height(2.dp)
                    .align(Alignment.BottomStart)
                    .background(NeteaseRed.copy(alpha = 0.5f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MiniBarHeight)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── 封面：播放时匀速旋转（黑胶）──
            MiniCover(song = song, isPlaying = state.isPlaying)

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
                Spacer(modifier = Modifier.height(2.dp))
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

/**
 * 迷你条封面：播放时匀速旋转，暂停时**停在当前角度**（不归零，否则会跳帧）。
 * 切歌时角度归零（新唱片从头转）。
 */
@Composable
private fun MiniCover(song: com.example.bna.data.model.Song, isPlaying: Boolean) {
    // 用无限动画驱动角度
    val infiniteTransition = rememberInfiniteTransition(label = "miniCoverSpin")
    val animatedRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "miniCoverRotation"
    )

    // ★ 暂停时冻结角度：记录"暂停瞬间"的角度，暂停期间显示它。
    // 若直接写 `if (isPlaying) rotation else 0f`，暂停会瞬间跳回 0°，观感是坏的。
    var frozenRotation by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (!isPlaying) frozenRotation = animatedRotation
    }
    // 切歌时把冻结角归零，避免新封面停在上一次的随机角度
    LaunchedEffect(song.id) { frozenRotation = 0f }
    val displayRotation = if (isPlaying) animatedRotation else frozenRotation

    Box(
        modifier = Modifier
            .size(MiniCoverSize)
            .clip(RoundedCornerShape(50))
            .background(DarkCard),
        contentAlignment = Alignment.Center
    ) {
        if (song.albumCoverUrl.isNotEmpty()) {
            AsyncImage(
                model = song.albumCoverUrl + "?param=80y80",
                contentDescription = song.albumName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = displayRotation }
            )
        } else {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = NeteaseRed,
                modifier = Modifier.size(20.dp)
            )
        }
        // 黑胶中心点（不旋转，始终居中）
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(DarkBackground.copy(alpha = 0.7f))
        )
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
                color = NeteaseRed,
                strokeWidth = 2.dp
            )
        }
        return
    }

    Box(
        modifier = Modifier
            .size(MiniPlayButtonSize)
            .clip(RoundedCornerShape(50))
            .background(NeteaseRed.copy(alpha = 0.15f))
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
                tint = NeteaseRed,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
