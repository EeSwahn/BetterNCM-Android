package com.example.bna.ui.screen.lyrics

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

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
