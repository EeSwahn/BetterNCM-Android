package com.example.bna.ui.screen.lyrics

import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 仿椒盐音乐风格的"流光"歌词背景：
 * 封面取色 -> 多层渐变 + 上浮光斑 -> 硬件模糊 -> 暗色 scrim。
 */
@Composable
fun FlowingLightBackground(coverUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // 每首歌只做一次颜色提取
    val paletteState = produceState<ExtractedPalette?>(initialValue = null, coverUrl) {
        value = extractPaletteFromCover(context, coverUrl)
    }
    val extracted = paletteState.value ?: DEFAULT_PALETTE

    // 换歌 600ms 淡入淡出：from -> to 用 Animatable 插值
    val mix = remember { Animatable(1f) }
    var fromPalette by remember { mutableStateOf<ExtractedPalette?>(null) }
    var toPalette by remember { mutableStateOf(extracted) }
    LaunchedEffect(extracted) {
        if (extracted != toPalette) {
            val start = fromPalette
            val end = toPalette
            // 当前混合状态作为新的 from，避免跳变
            fromPalette = if (start == null) end else blendPalettes(
                start, end, mix.value
            )
            toPalette = extracted
            mix.snapTo(0f)
            mix.animateTo(1f, animationSpec = tween(durationMillis = 600))
        }
    }
    val from = fromPalette
    val blendedColors: List<Color> =
        if (from == null || mix.value >= 0.999f) {
            toPalette.accentColors
        } else {
            List(toPalette.accentColors.size) { i ->
                val fc = from.accentColors.getOrElse(i) { toPalette.accentColors[i] }
                lerp(fc, toPalette.accentColors[i], mix.value)
            }
        }
    val blendedBase: Color =
        if (from == null || mix.value >= 0.999f) {
            toPalette.baseColor
        } else {
            lerp(from.baseColor, toPalette.baseColor, mix.value)
        }

    // 光斑动画：每个光斑独立的无限循环 0f -> 1f（上浮进度）
    val infiniteTransition = rememberInfiniteTransition(label = "floatBlobs")
    val blobs = remember(coverUrl) {
        val rnd = Random(coverUrl.hashCode())
        List(BLOB_COUNT) {
            BlobSpec(
                xFraction = 0.08f + rnd.nextFloat() * 0.84f,
                phaseOffset = rnd.nextFloat(),
                durationMs = 14_000 + rnd.nextInt(12_001), // 14~26s
                sizeFraction = 0.30f + rnd.nextFloat() * 0.25f, // 屏宽 30%~55%
                breathPhase = rnd.nextFloat() * (2f * PI.toFloat())
            )
        }
    }
    val progresses = blobs.mapIndexed { index, blob ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(blob.durationMs, easing = LinearEasing)
            ),
            label = "blob$index"
        )
    }
    // 呼吸节奏与上浮用同一个帧时钟，避免相位抖动
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing)
        ),
        label = "breath"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // 光斑 + 渐变图层（整体硬件模糊）
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(28.dp, BlurredEdgeTreatment.Unbounded)
        ) {
            // 背景底：垂直渐变（顶部基底色稍亮 -> 底部近黑）
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        blendedBase,
                        lerp(blendedBase, Color.Black, 0.35f),
                        Color(0xFF05060A)
                    )
                )
            )

            blobs.forEachIndexed { i, blob ->
                val progress = progresses[i].value
                val accent = blendedColors[i % blendedColors.size]

                // 从屏幕下方 1.35 上浮到上方 -0.35，行程加大保证循环点完全出屏
                val yFraction = 1.35f - progress * 1.7f
                // 出入场渐隐：两端 alpha=0，重启/起点无跳变
                val edgeFade = sin(progress * PI.toFloat())
                // 呼吸：半径 ±15% 正弦（与上浮共用同一帧时钟）
                val breath = 1f + 0.15f * sin(breathPhase + blob.breathPhase)
                val radius = size.width * blob.sizeFraction * breath

                val cx = size.width * blob.xFraction
                val cy = size.height * yFraction

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.85f * edgeFade),
                            accent.copy(alpha = 0f)
                        ),
                        center = Offset(cx, cy),
                        radius = radius
                    ),
                    blendMode = BlendMode.Screen
                )
            }
        }

        // 暗色 scrim，保证歌词可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.32f),
                                Color.Black.copy(alpha = 0.62f)
                            )
                        )
                    )
                }
        )
    }
}

private const val BLOB_COUNT = 6

private data class BlobSpec(
    val xFraction: Float,
    val phaseOffset: Float,
    val durationMs: Int,
    val sizeFraction: Float,
    val breathPhase: Float
)

private data class ExtractedPalette(
    val baseColor: Color,
    val accentColors: List<Color>
)

private val DEFAULT_PALETTE = ExtractedPalette(
    baseColor = Color(0xFF141A26),
    accentColors = listOf(
        Color(0xFF3A5A8C),
        Color(0xFF4A4E69),
        Color(0xFF2E4057),
        Color(0xFF5C6B8A)
    )
)

/** 自研量化取色（禁止 androidx.palette）。 */
private suspend fun extractPaletteFromCover(
    context: android.content.Context,
    coverUrl: String
): ExtractedPalette = withContext(Dispatchers.Default) {
    try {
        val request = ImageRequest.Builder(context)
            .data(if (coverUrl.isEmpty()) null else coverUrl + "?param=300y300")
            .allowHardware(false)
            .size(96)
            .build()
        val result = context.imageLoader.execute(request)
        if (result !is SuccessResult) return@withContext DEFAULT_PALETTE
        val drawable = result.drawable
        val src = (drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
            ?: drawableToBitmap(drawable)
        val bmp = Bitmap.createScaledBitmap(src, 96, 96, true)

        val pixels = IntArray(96 * 96)
        bmp.getPixels(pixels, 0, 96, 0, 0, 96, 96)

        // 按色相分 24 桶统计
        val bucketCount = 24
        val bucketR = DoubleArray(bucketCount)
        val bucketG = DoubleArray(bucketCount)
        val bucketB = DoubleArray(bucketCount)
        val bucketWeight = DoubleArray(bucketCount)
        var totalWeight = 0.0
        val hsv = FloatArray(3)

        for (px in pixels) {
            val a = px ushr 24 and 0xFF
            if (a < 200) continue // 剔除透明
            val r = px shr 16 and 0xFF
            val g = px shr 8 and 0xFF
            val b = px and 0xFF
            // 剔除近乎纯黑白灰
            val maxc = maxOf(r, g, b)
            val minc = minOf(r, g, b)
            if (maxc - minc < 18) continue
            if (maxc < 30 || minc > 235) continue

            android.graphics.Color.colorToHSV(px, hsv)
            val bucket = (hsv[0] / 360f * bucketCount).toInt().coerceIn(0, bucketCount - 1)
            // 饱和度与亮度加权
            val w = (0.25 + hsv[1]) * (0.25 + hsv[2])
            bucketR[bucket] += r * w
            bucketG[bucket] += g * w
            bucketB[bucket] += b * w
            bucketWeight[bucket] += w
            totalWeight += w
        }

        if (totalWeight <= 0.0) return@withContext DEFAULT_PALETTE

        // 取占比最高的 3~5 个桶
        val sorted = (0 until bucketCount)
            .filter { bucketWeight[it] > 0 }
            .sortedByDescending { bucketWeight[it] }
            .take(5)
            .filter { bucketWeight[it] / totalWeight >= 0.03 }
            .takeIf { it.isNotEmpty() } ?: return@withContext DEFAULT_PALETTE

        val accents = sorted.map { bucket ->
            val w = bucketWeight[bucket]
            val r = (bucketR[bucket] / w).toInt().coerceIn(0, 255)
            val g = (bucketG[bucket] / w).toInt().coerceIn(0, 255)
            val b = (bucketB[bucket] / w).toInt().coerceIn(0, 255)
            android.graphics.Color.colorToHSV(
                android.graphics.Color.rgb(r, g, b), hsv
            )
            // 饱和度提升 1.35 倍并夹取；亮度夹取 0.25~0.88
            hsv[1] = (hsv[1] * 1.35f).coerceAtMost(1f)
            hsv[2] = hsv[2].coerceIn(0.25f, 0.88f)
            Color(android.graphics.Color.HSVToColor(hsv))
        }

        // 主代表色压暗为基底色
        val main = accents[0]
        android.graphics.Color.colorToHSV(main.toArgb(), hsv)
        hsv[2] *= 0.48f
        val base = Color(android.graphics.Color.HSVToColor(hsv))

        ExtractedPalette(baseColor = base, accentColors = accents)
    } catch (t: Throwable) {
        DEFAULT_PALETTE
    }
}

private fun blendPalettes(
    from: ExtractedPalette,
    to: ExtractedPalette,
    m: Float
): ExtractedPalette {
    val colors = List(to.accentColors.size) { i ->
        lerp(from.accentColors.getOrElse(i) { to.accentColors[i] }, to.accentColors[i], m)
    }
    return ExtractedPalette(lerp(from.baseColor, to.baseColor, m), colors)
}

private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
    val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
    val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawable.setBounds(0, 0, w, h)
    drawable.draw(canvas)
    return bmp
}
