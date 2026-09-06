package com.example.bna.ui.screen.lyrics

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.bna.ui.theme.DarkCard
import com.example.bna.ui.theme.NeteaseRed
import kotlin.math.abs

/**
 * 歌词页共享的「视觉内核」：色取 / 低音边缘辉光 / 呼吸发光封面。
 *
 * 原手机/平板两套布局各自内联几乎相同的实现，这里收敛成几个语义组合，
 * 两套外壳（pager 竖流 / 左右分栏）只负责摆放，不再复制渲染细节。
 */

/**
 * 从封面缩略图提取一次主导色（发光源颜色），换歌后自动重新提取。
 */
@Composable
fun rememberCoverDominantColor(coverUrl: String): Color {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color(0xFF00BFFF)) }
    LaunchedEffect(coverUrl) {
        if (coverUrl.isNotEmpty()) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverUrl + "?param=200y200")
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        dominantColor = extractVibrantColor(bitmap)
                    }
                }
            } catch (_: Exception) { }
        }
    }
    return dominantColor
}

/**
 * 屏幕右侧边缘随低音脉冲的辉光覆盖层。
 *
 * 内部负责轮询随想唱处理器的实时振幅（带历史延迟补偿），并按鼓点阈值
 * 计算辉光 alpha，用 [dominantColor] 描一条经过屏幕右缘上下的软边。
 * 应置于整套布局最外层，Phone / Tablet 直接复用。
 */
@Composable
fun BeatEdgeGlowOverlay(
    dominantColor: Color,
    modifier: Modifier = Modifier,
    enableEdgeGlow: Boolean = true,
    rightEdgeGlowRadius: Float = 98f,
    beatGlowThreshold: Float = 0.1f,
    beatGlowDelayMs: Float = 352.9f
) {
    var audioAmplitude by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val history = mutableListOf<Pair<Long, Float>>()
        while (true) {
            val now = System.currentTimeMillis()
            val currentAmp = com.example.bna.player.MusicPlayer.suiXinChangProcessor.currentAmplitude
            history.add(now to currentAmp)
            history.removeAll { now - it.first > 2000 } // 保留最近 2 秒历史
            val targetTime = now - beatGlowDelayMs.toLong()
            val delayedAmp = history.minByOrNull { abs(it.first - targetTime) }?.second ?: currentAmp
            audioAmplitude = delayedAmp
            kotlinx.coroutines.delay(16) // 16ms 轮询，接近 60fps
        }
    }
    val targetEdgeAlpha = if (audioAmplitude > beatGlowThreshold) 0.2f + audioAmplitude * 0.8f else 0f
    val animatedEdgeAlpha by animateFloatAsState(
        targetValue = targetEdgeAlpha,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "edgeAlphaAnim"
    )
    if (enableEdgeGlow && rightEdgeGlowRadius > 0f) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .alpha(animatedEdgeAlpha)
                .blur(30.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w, 0f)
                quadraticTo(w - 2 * rightEdgeGlowRadius, h / 2f, w, h)
                close()
            }
            drawPath(path = path, color = dominantColor.copy(alpha = 0.5f))
        }
    }
}

/**
 * 带呼吸光晕 + 边缘渐隐的封面本体。
 *
 * [coverSize] 是唯一决定封面的尺寸参数（Salt 风格：由外壳用屏幕约束算好传入），
 * 圆角/阴影从 [LocalLyricsDimens] 读取。Phone / Tablet 共用同一渲染。
 */
@Composable
fun GlowingCover(
    coverUrl: String,
    songName: String,
    coverSize: Dp,
    modifier: Modifier = Modifier,
    glowBrightness: Float = 0.09f,
    glowBreathFrequency: Float = 0.5f,
    glowScaleSize: Float = 1.3f,
    showShadow: Boolean = true,
    cornerRadius: Dp = androidx.compose.ui.unit.Dp.Unspecified,
    placeholderSize: Dp = 80.dp
) {
    val radius = if (cornerRadius == androidx.compose.ui.unit.Dp.Unspecified) {
        LocalLyricsDimens.current.coverCornerRadius
    } else cornerRadius
    val shape = RoundedCornerShape(radius)

    val breathAnim = remember { Animatable(1.0f) }
    LaunchedEffect(glowBreathFrequency) {
        if (glowBreathFrequency > 0.01f) {
            val periodMs = (1000f / glowBreathFrequency).toInt().coerceAtLeast(200)
            while (true) {
                breathAnim.animateTo(
                    targetValue = 0.45f,
                    animationSpec = tween(durationMillis = periodMs)
                )
                breathAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = periodMs)
                )
            }
        } else {
            breathAnim.snapTo(1.0f)
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 发光层：使用低分辨率缩略图做高斯模糊，避免模糊原图消耗性能
        if (glowBrightness > 0.01f && coverUrl.isNotEmpty()) {
            val colorMatrix = remember { ColorMatrix().apply { setToSaturation(1.4f) } }
            AsyncImage(
                model = coverUrl + "?param=32y32",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                modifier = Modifier
                    .requiredSize(coverSize * glowScaleSize)
                    .clip(RoundedCornerShape(radius * glowScaleSize))
                    .graphicsLayer {
                        alpha = 0.6f * breathAnim.value
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val fadeFraction = 0.15f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.Transparent,
                                fadeFraction to Color.Black,
                                1f - fadeFraction to Color.Black,
                                1.0f to Color.Transparent,
                                startX = 0f,
                                endX = size.width
                            ),
                            blendMode = BlendMode.DstIn
                        )
                        drawRect(
                            brush = Brush.verticalGradient(
                                0.0f to Color.Transparent,
                                fadeFraction to Color.Black,
                                1f - fadeFraction to Color.Black,
                                1.0f to Color.Transparent,
                                startY = 0f,
                                endY = size.height
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .blur(48.dp, BlurredEdgeTreatment.Unbounded)
            )
        }

        // 封面本体
        val coverBox = @Composable {
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), shape)
            ) {
                if (coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = coverUrl + "?param=800y800",
                        contentDescription = songName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeteaseRed,
                            modifier = Modifier.size(placeholderSize)
                        )
                    }
                }
            }
        }

        if (showShadow) {
            Box(
                modifier = Modifier
                    .size(coverSize)
                    .shadow(
                        elevation = 28.dp,
                        shape = shape,
                        spotColor = Color.Black.copy(alpha = 0.55f)
                    )
            ) {
                coverBox()
            }
        } else {
            coverBox()
        }
    }
}
