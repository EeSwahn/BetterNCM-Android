package com.example.bna.ui.animation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*

@Composable
fun Modifier.lyrics3DEffect(
    isCurrent: Boolean,
    distance: Int
): Modifier = then(
    Modifier.graphicsLayer {
        if (isCurrent) {
            rotationX = 0f
            rotationY = 0f
            rotationZ = 0f
            cameraDistance = 12f
            alpha = 1f
        } else {
            val maxDistance = 5
            val normalizedDistance = (distance.toFloat() / maxDistance).coerceIn(0f, 1f)
            
            rotationX = 0f
            rotationY = 0f
            rotationZ = 0f
            cameraDistance = 12f
            alpha = 1f - normalizedDistance * 0.5f
        }
    }
)

data class LyricsAnimationConfig(
    val enable3DEffect: Boolean = true,
    val enableFlowingLight: Boolean = true,
    val enableGlowEffect: Boolean = true,
    val reduceFlowingLightEffect: Boolean = false,
    val flowingLightMode: Int = 0
)

@Composable
fun rememberLyricsAnimationConfig(): LyricsAnimationConfig = LyricsAnimationConfig()
