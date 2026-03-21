package com.example.bna.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.player.MusicPlayer
import com.example.bna.player.PlayerState
import com.example.bna.ui.theme.*

@Composable
fun PlayerBar(onClick: () -> Unit) {
    val playerState by MusicPlayer.playerState.collectAsState()

    AnimatedVisibility(
        visible = playerState.currentSong != null,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        PlayerBarContent(state = playerState, onClick = onClick)
    }
}

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

        // 背景进度条（模拟视觉效果）
        if (state.duration > 0) {
            val progress = (state.currentPosition.toFloat() / state.duration).coerceIn(0f, 1f)
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 封面（旋转效果）
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    )
                } else {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = NeteaseRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // 黑胶中心点
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(DarkBackground.copy(alpha = 0.7f))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲信息
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

            // 加载指示器 / 播放控制
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = NeteaseRed,
                    strokeWidth = 2.dp
                )
            } else {
                // 播放 / 暂停
                IconButton(
                    onClick = { MusicPlayer.togglePlayPause() },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NeteaseRed.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.isPlaying) "暂停" else "播放",
                        tint = NeteaseRed,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // 歌曲结束 / 清空（预留）
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
