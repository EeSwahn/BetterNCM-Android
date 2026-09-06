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

@Composable
fun Modifier.flowingLightEffect(
    isCurrent: Boolean,
    isPlaying: Boolean,
    reduceEffect: Boolean = false
): Modifier {
    if (!isCurrent || !isPlaying) {
        return this
    }
    
    // Gradient overlay disabled to remove rectangular shading on the active line.
    return this.then(
        Modifier.drawWithContent {
            drawContent()
        }
    )
}

@Composable
fun Modifier.glowEffect(
    isCurrent: Boolean,
    glowColor: Color = Color.White,
    maxRadius: Float = 24f,
    maxAlpha: Float = 0.6f
): Modifier {
    if (!isCurrent) {
        return this
    }
    
    // We remove the square box drawing. Real text glow should be done via TextStyle shadows.
    return this.then(
        Modifier.drawWithContent {
            drawContent()
        }
    )
}


data class LyricsAnimationConfig(
    val enable3DEffect: Boolean = true,
    val enableFlowingLight: Boolean = true,
    val enableGlowEffect: Boolean = true,
    val reduceFlowingLightEffect: Boolean = false,
    val flowingLightMode: Int = 0
)

@Composable
fun rememberLyricsAnimationConfig(): LyricsAnimationConfig = LyricsAnimationConfig()
