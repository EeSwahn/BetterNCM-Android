package com.example.bna.ui.screen.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

/**
 * 以 1/[scale] 分辨率离屏绘制内容，再放大合成。
 * 内部 Density 同步缩小，dp / blur 半径等换算后与全分辨率视觉一致，
 * 但模糊等全屏像素级操作的开销约降为 1/scale²。仅用于本身就很柔和的内容。
 * [alpha] 在合成阶段读取，动画期间不触发重组或重绘内容。
 */
@Composable
fun LowResLayer(
    modifier: Modifier = Modifier,
    scale: Int = 4,
    alpha: () -> Float = { 1f },
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val lowDensity = remember(density, scale) { Density(density.density / scale, density.fontScale) }
    Layout(
        content = { CompositionLocalProvider(LocalDensity provides lowDensity) { content() } },
        modifier = modifier
    ) { measurables, constraints ->
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val smallW = (w / scale).coerceAtLeast(1)
        val smallH = (h / scale).coerceAtLeast(1)
        val placeables = measurables.map { it.measure(Constraints.fixed(smallW, smallH)) }
        val sx = w.toFloat() / smallW
        val sy = h.toFloat() / smallH
        layout(w, h) {
            placeables.forEach { p ->
                p.placeWithLayer(0, 0) {
                    scaleX = sx
                    scaleY = sy
                    transformOrigin = TransformOrigin(0f, 0f)
                    compositingStrategy = CompositingStrategy.Offscreen
                    this.alpha = alpha()
                }
            }
        }
    }
}
