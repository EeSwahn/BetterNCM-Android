package com.example.mymusic.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.example.mymusic.viewmodel.SettingsViewModel

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
fun Modifier.clickBounceEffect(
    isPressed: Boolean
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )
    
    return this.then(
        Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
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

@Composable
fun Modifier.smoothScrollEffect(
    offset: Float
): Modifier {
    return this.then(
        Modifier.graphicsLayer {
            translationY = offset
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
fun rememberLyricsAnimationConfig(
    settingsViewModel: SettingsViewModel? = null
): LyricsAnimationConfig {
    val animationState = settingsViewModel?.animationState?.collectAsState()?.value
    
    return remember(animationState) {
        LyricsAnimationConfig(
            enable3DEffect = animationState?.enable3DEffect ?: true,
            enableFlowingLight = animationState?.enableFlowingLight ?: true,
            enableGlowEffect = animationState?.enableGlowEffect ?: true,
            reduceFlowingLightEffect = animationState?.reduceFlowingLightEffect ?: false,
            flowingLightMode = animationState?.flowingLightMode ?: 0
        )
    }
}
