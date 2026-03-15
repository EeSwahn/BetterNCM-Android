package com.example.mymusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NeteaseRed,
    onPrimary = TextPrimary,
    primaryContainer = NeteaseRedDark,
    onPrimaryContainer = TextPrimary,
    secondary = AccentGold,
    onSecondary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = NeteaseRedLight,
    onError = TextPrimary,
    outline = DividerColor,
    outlineVariant = DarkElevated
)

@Composable
fun MyMusicTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}