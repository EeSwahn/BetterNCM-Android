package com.example.mymusic.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.mymusic.ui.theme.DarkBackground

@Composable
fun MainScreen(onLogout: () -> Unit = {}) {
    var showLyricsScreen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            bottomBar = {
                PlayerBar(onClick = { showLyricsScreen = true })
            }
        ) { innerPadding ->
            SearchScreen(
                modifier = Modifier.padding(innerPadding),
                onLogout = onLogout
            )
        }

        AnimatedVisibility(
            visible = showLyricsScreen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            LyricsScreen(onDismiss = { showLyricsScreen = false })
        }
    }
}
