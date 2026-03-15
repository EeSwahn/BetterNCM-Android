package com.example.mymusic.ui.screen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mymusic.ui.theme.NeteaseRed
import com.example.mymusic.ui.theme.TextPrimary
import com.example.mymusic.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onLoginValid: () -> Unit,
    onNeedLogin: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val uiState by loginViewModel.uiState.collectAsState()

    // 最短展示时长保证（让启动画面至少显示 600ms）
    val minSplashDone = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        println("DEBUG: SplashScreen started")
        delay(600)
        minSplashDone.value = true
    }

    // 同时满足：最短时间已到 & 网络检查已完成，再路由
    LaunchedEffect(minSplashDone.value, uiState.isLoading) {
        if (minSplashDone.value && !uiState.isLoading) {
            println("DEBUG: Check done - isLoggedIn: ${uiState.isLoggedIn}")
            if (uiState.isLoggedIn) {
                println("DEBUG: Navigating to main screen")
                onLoginValid()
            } else {
                println("DEBUG: Navigating to login screen")
                onNeedLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0000),
                        Color(0xFF0D0D0D)
                    )
                )
            )
    ) {
        SplashContent()
    }
}

@Composable
private fun SplashContent() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_anim")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rotation"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                NeteaseRed.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(NeteaseRed, Color(0xFFB71C1C))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "My Music",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "聆听世界，感受音乐",
            fontSize = 14.sp,
            color = Color(0xFFB0B0B0)
        )
    }
}
