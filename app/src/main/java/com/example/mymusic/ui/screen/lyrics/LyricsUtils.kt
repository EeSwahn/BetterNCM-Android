package com.example.mymusic.ui.screen.lyrics

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.mymusic.data.model.LyricLine
import com.example.mymusic.data.model.WordInfo

@Composable
fun rememberFloatPreference(key: String, defaultValue: Float): MutableState<Float> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lyrics_settings", Context.MODE_PRIVATE) }
    val state = remember { mutableStateOf(prefs.getFloat(key, defaultValue)) }
    
    return remember {
        object : MutableState<Float> {
            override var value: Float
                get() = state.value
                set(v) {
                    state.value = v
                    prefs.edit().putFloat(key, v).apply()
                }
            override fun component1() = state.value
            override fun component2(): (Float) -> Unit = { v: Float -> value = v }
        }
    }
}

@Composable
fun rememberBooleanPreference(key: String, defaultValue: Boolean): MutableState<Boolean> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("lyrics_settings", Context.MODE_PRIVATE) }
    val state = remember { mutableStateOf(prefs.getBoolean(key, defaultValue)) }
    
    return remember {
        object : MutableState<Boolean> {
            override var value: Boolean
                get() = state.value
                set(v) {
                    state.value = v
                    prefs.edit().putBoolean(key, v).apply()
                }
            override fun component1() = state.value
            override fun component2(): (Boolean) -> Unit = { v: Boolean -> value = v }
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

fun getDetailedLyricsProgress(currentPosition: Long, line: LyricLine): Float {
    if (line.words == null || line.words.isEmpty()) {
        return ((currentPosition - line.timeMs).toFloat() / line.durationMs.coerceAtLeast(1L)).coerceIn(0f, 1f)
    }
    
    val relativePos = (currentPosition - line.timeMs).toInt()
    if (relativePos <= 0) return 0f
    
    val words = line.words
    val totalChars = line.text.length
    if (totalChars == 0) return 0f
    
    var charsProcessed = 0
    for (word in words) {
        val wordEnd = word.startOffset + word.duration
        if (relativePos < word.startOffset) {
            return charsProcessed.toFloat() / totalChars
        }
        if (relativePos < wordEnd) {
            val wordFactor = (relativePos - word.startOffset).toFloat() / word.duration.coerceAtLeast(1)
            val currentChars = charsProcessed + (word.text.length * wordFactor)
            return (currentChars / totalChars).coerceIn(0f, 1f)
        }
        charsProcessed += word.text.length
    }
    return 1f
}

fun generateWordInfoForLine(line: LyricLine): List<WordInfo> {
    val text = line.text
    if (text.isEmpty()) return emptyList()
    
    val totalDuration = line.durationMs.coerceAtLeast(100L).toInt()
    val charCount = text.length.coerceAtLeast(1)
    val durationPerChar = (totalDuration / charCount).coerceAtLeast(10)
    
    val result = mutableListOf<WordInfo>()
    var currentOffset = 0
    
    text.forEachIndexed { _, char ->
        result.add(
            WordInfo(
                startOffset = currentOffset,
                duration = durationPerChar,
                text = char.toString()
            )
        )
        currentOffset += durationPerChar
    }
    
    return result
}
