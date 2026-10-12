package com.example.bna.ui.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bna.ui.screen.lyrics.*

// Scrollable trailing space, never a fixed footer that truncates the viewport.
internal val LocalHomeListBottomPadding = staticCompositionLocalOf { 24.dp }

internal val HomeEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
internal data class HomeColors(val background: Color, val surface: Color, val accent: Color)
internal val LocalHomeColors = staticCompositionLocalOf {
    HomeColors(Color(0xFF10141D), Color(0xFF242B3B), Color(0xFFB9C9EF))
}

@Composable
internal fun rememberHomeColors(coverUrl: String): HomeColors {
    val context = LocalContext.current
    val palette by produceState(DEFAULT_PALETTE, coverUrl) {
        value = extractPaletteFromCover(context, coverUrl)
    }
    val accent = palette.accentColors.firstOrNull() ?: Color(0xFF779BD1)
    val background by animateColorAsState(lerp(palette.baseColor, Color(0xFF0D1119), 0.7f), tween(600), label = "homeBase")
    val surface by animateColorAsState(lerp(accent, Color(0xFF1B202C), 0.8f), tween(600), label = "homeSurface")
    val highlight by animateColorAsState(lerp(accent, Color.White, 0.55f), tween(600), label = "homeAccent")
    return HomeColors(background, surface, highlight)
}

internal class CoverFlight {
    var artwork by mutableStateOf<androidx.compose.ui.graphics.painter.Painter?>(null)
    var mini by mutableStateOf<Rect?>(null)
    var expanded by mutableStateOf<Rect?>(null)
    var active by mutableStateOf(false)
}
internal val LocalCoverFlight = staticCompositionLocalOf<CoverFlight?> { null }

internal fun Modifier.flightCover(expanded: Boolean): Modifier = composed {
    val flight = LocalCoverFlight.current
    this.onGloballyPositioned {
        val bounds = it.boundsInRoot()
        if (expanded) flight?.expanded = bounds else flight?.mini = bounds
    }.graphicsLayer { alpha = if (flight?.active == true) 0f else 1f }
}

internal fun Modifier.homePress(source: MutableInteractionSource): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.965f else 1f,
        tween(if (pressed) 100 else 260, easing = HomeEase), label = "navigationPress")
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}
