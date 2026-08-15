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

/**
 * 自定义快速封面颜色提取：
 * 极度缩小图片后计算亮度和色彩度得分，选取最亮最鲜艳的像素颜色。
 */
fun extractVibrantColor(bitmap: android.graphics.Bitmap): androidx.compose.ui.graphics.Color {
    // 1. 极度缩小到 16x16，将几百万像素降至 256 个，性能开销极小
    val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(bitmap, 16, 16, true)
    
    var bestColor = android.graphics.Color.WHITE
    var maxScore = -1f
    
    for (x in 0 until scaledBitmap.width) {
        for (y in 0 until scaledBitmap.height) {
            val pixel = scaledBitmap.getPixel(x, y)
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            
            val l = 0.299f * r + 0.587f * g + 0.114f * b
            val maxColor = maxOf(r, maxOf(g, b))
            val minColor = minOf(r, minOf(g, b))
            
            val v = maxColor
            val s = if (v == 0) 0f else (v - minColor).toFloat() / v
            
            // V < 130 拦截：发光的光源必须自己是明亮的
            if (v < 130) continue
            
            // S < 0.5 拦截：过滤偏灰的杂色泥巴色
            if (s < 0.5f) continue
            
            // 针对性补丁：拦截暗淡的土味黄棕色
            if (r > g && b < 60 && v < 220) continue
            
            val c = (maxColor - minColor).toFloat()
            val score = l * c
            if (score > maxScore) {
                maxScore = score
                bestColor = pixel
            }
        }
    }
    
    // 如果所有像素都被剔除了（比如纯黑白封面），退化为单纯找最亮的像素
    if (maxScore == -1f) {
        for (x in 0 until scaledBitmap.width) {
            for (y in 0 until scaledBitmap.height) {
                val pixel = scaledBitmap.getPixel(x, y)
                val r = android.graphics.Color.red(pixel)
                val g = android.graphics.Color.green(pixel)
                val b = android.graphics.Color.blue(pixel)
                val l = 0.299f * r + 0.587f * g + 0.114f * b
                if (l > maxScore) {
                    maxScore = l
                    bestColor = pixel
                }
            }
        }
    }
    
    // 终极屎色拦截器：如果算出来的颜色依然是“屎色”（土褐、暗黄、泥巴灰），直接强行替换为纯白色
    val finalR = android.graphics.Color.red(bestColor)
    val finalG = android.graphics.Color.green(bestColor)
    val finalB = android.graphics.Color.blue(bestColor)
    val finalV = maxOf(finalR, maxOf(finalG, finalB))
    val finalMin = minOf(finalR, minOf(finalG, finalB))
    val finalS = if (finalV == 0) 0f else (finalV - finalMin).toFloat() / finalV
    
    val isPoop = (finalR > finalG && finalG >= finalB && finalB < 100 && finalV < 220) || 
                 (finalR > finalG && finalB < 80 && finalV < 210) ||
                 (finalS < 0.5f && finalV < 180) ||
                 (finalV < 130) // 太暗的也直接当黑/白色处理（发光本身不能是暗色）
                 
    if (isPoop) {
        bestColor = android.graphics.Color.WHITE
    }
    
    return androidx.compose.ui.graphics.Color(bestColor)
}

/**
 * 平板设置面板全部设置项登记表（分组 + 键名 + 代码默认值）。
 * 导出时逐项读取生效值，保证未动过的滑块也能按默认值导出。
 */
data class LyricsSettingDef(val key: String, val defaultValue: Float)

val tabletLyricsSettingGroups: List<Pair<String, List<LyricsSettingDef>>> = listOf(
    "封面发光" to listOf(
        LyricsSettingDef("glowBrightness", 0.09f),
        LyricsSettingDef("glowBreathFrequency", 0.5f),
        LyricsSettingDef("glowScaleSize", 1.3f),
        LyricsSettingDef("rightEdgeGlowRadius", 98.0f),
        LyricsSettingDef("beatGlowThreshold", 0.1f)
    ),
    "歌词动画" to listOf(
        LyricsSettingDef("verticalScrollSpeed", 0.5f),
        LyricsSettingDef("scaleAnimationSpeed", 0.5f),
        LyricsSettingDef("activeLyricSizeRatio", 0.7f),
        LyricsSettingDef("baseFontSizeRatio", 1.3f),
        LyricsSettingDef("lineSpacingRatio", 0.5f)
    ),
    "逐字细节" to listOf(
        LyricsSettingDef("yrcFloatSpeed", 2.0f),
        LyricsSettingDef("yrcFloatIntensity", 3.92f),
        LyricsSettingDef("wordTimingOffsetMs", 0f),
        LyricsSettingDef("wordScaleSpeed", 0.27f),
        LyricsSettingDef("wordScaleSize", 1.0f)
    ),
    "按钮调节" to listOf(
        LyricsSettingDef("playbackButtonSizeRatio", 0.6888889f),
        LyricsSettingDef("playbackButtonSpacingDp", 0f),
        LyricsSettingDef("bottomButtonSizeRatio", 0.6f),
        LyricsSettingDef("bottomButtonSpacingDp", 0f)
    ),
    "左侧布局校准" to listOf(
        LyricsSettingDef("coverSizeRatio", 1.5f),
        LyricsSettingDef("headerOffsetX", 0f),
        LyricsSettingDef("headerOffsetY", 35.966827f),
        LyricsSettingDef("coverOffsetX", 0f),
        LyricsSettingDef("coverOffsetY", 50.522354f),
        LyricsSettingDef("audioSpecOffsetX", 0f),
        LyricsSettingDef("audioSpecOffsetY", 58.030396f),
        LyricsSettingDef("playbackOffsetX", 0f),
        LyricsSettingDef("playbackOffsetY", 46.11606f),
        LyricsSettingDef("bottomOffsetX", 0f),
        LyricsSettingDef("bottomOffsetY", 40.814377f)
    ),
    "右侧布局校准" to listOf(
        LyricsSettingDef("lyricsPanelOffsetX", 0f),
        LyricsSettingDef("lyricsPanelOffsetY", 0f),
        LyricsSettingDef("progressBarOffsetX", 0f),
        LyricsSettingDef("progressBarOffsetY", 53.176422f),
        LyricsSettingDef("progressBarWidthRatio", 1.2444445f)
    )
)

val tabletLyricsBooleanSettings: List<Pair<String, Boolean>> = listOf(
    "enableWordByWord" to true
)

/**
 * 把设置面板的全部设置项导出到应用外部文件目录。
 * 以登记表为准逐项导出：已调过的写生效值，没动过的写代码默认值并标注 (默认)。
 * 路径通常为 /sdcard/Android/data/<包名>/files/，无需存储权限。
 */
fun exportLyricsSettings(context: Context): java.io.File {
    val prefs = context.getSharedPreferences("lyrics_settings", Context.MODE_PRIVATE)
    val dir = context.getExternalFilesDir(null) ?: context.filesDir
    val file = java.io.File(dir, "lyrics_settings_export.txt")
    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        .format(java.util.Date())
    val sb = StringBuilder()
    sb.appendLine("# BNA 歌词界面设置导出（平板）")
    sb.appendLine("# 导出时间: $timestamp")
    sb.appendLine("# 标注 (默认) 的为未手动调节、使用代码默认值的项")

    tabletLyricsBooleanSettings.forEach { (key, default) ->
        val modified = prefs.contains(key)
        val value = if (modified) prefs.getBoolean(key, default) else default
        sb.appendLine("$key = $value${if (modified) "" else "  (默认)"}")
    }

    tabletLyricsSettingGroups.forEach { (group, defs) ->
        sb.appendLine()
        sb.appendLine("[$group]")
        defs.forEach { def ->
            val modified = prefs.contains(def.key)
            val value = if (modified) prefs.getFloat(def.key, def.defaultValue) else def.defaultValue
            sb.appendLine("${def.key} = $value${if (modified) "" else "  (默认)"}")
        }
    }

    file.writeText(sb.toString())
    return file
}
