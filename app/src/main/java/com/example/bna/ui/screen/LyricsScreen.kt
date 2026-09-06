package com.example.bna.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bna.player.MusicPlayer
import com.example.bna.ui.animation.*
import com.example.bna.ui.theme.*
import com.example.bna.ui.screen.lyrics.*
import com.example.bna.viewmodel.LyricsViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay

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
                onClick = {}
            )
    ) {
        if (song != null) {
            FlowingLightBackground(
                coverUrl = song.albumCoverUrl
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
