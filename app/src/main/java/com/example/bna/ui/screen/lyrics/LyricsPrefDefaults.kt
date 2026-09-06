package com.example.bna.ui.screen.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

/**
 * 歌词页「可调滑块」的集中默认表（单一来源）。
 *
 * 目标：让 Phone / Tablet 两套外壳不再各自写一遍同一批 key 的默认值，
 * 也避免同一 key 在两处默认不同导致漂移。这里集中定义：
 *  - key
 *  - 按形态档（手机/平板）区分的默认值（部分 key 两端默认本就不同）
 *  - 滑块取值范围 + 刻度数（供设置 UI 与将来的导出/固化用）
 *  - 语义注释
 *
 * 「填表式固化」流程：agent 拿到与这张表同构的导出文件后，只改这里的
 * `phoneDefault` / `tabletDefault` 即可，全局生效、随 APK 分发。
 */

enum class LyricsFormFactor { Phone, Tablet }

/** 单个浮点滑块的完整定义。 */
data class FloatPrefSpec(
    val key: String,
    val phoneDefault: Float,
    val tabletDefault: Float,
    val min: Float,
    val max: Float,
    val steps: Int = 0,
    val label: String = key,
    val description: String = ""
)

/** 单个布尔开关的完整定义。 */
data class BoolPrefSpec(
    val key: String,
    val phoneDefault: Boolean,
    val tabletDefault: Boolean,
    val label: String = key,
    val description: String = ""
)

object LyricsPrefDefaults {

    /** 浮点滑块清单（顺序即导出/设置页展示顺序）。 */
    val floats: List<FloatPrefSpec> = listOf(
        // ---- 封面发光 ----
        FloatPrefSpec("glowBrightness", 0.09f, 0.09f, 0f, 1f, 10, "发光亮度", "控制封面光晕的整体亮度，为 0 时关闭发光。"),
        FloatPrefSpec("glowScaleSize", 1.3f, 1.3f, 1f, 3f, 20, "发光大小", "控制封面下方发光图层的缩放比例。"),
        FloatPrefSpec("glowBreathFrequency", 0.5f, 0.5f, 0f, 5f, 10, "呼吸频率", "发光明暗交替的速度，为 0 时光晕保持静态。"),
        FloatPrefSpec("rightEdgeGlowRadius", 98f, 98f, 0f, 500f, 50, "右边缘发光", "控制右侧边缘发光的半径。无论怎么调整，发光始终经过右侧上下两点。"),
        FloatPrefSpec("beatGlowThreshold", 0.1f, 0.1f, 0f, 1f, 50, "发光鼓点阈值", "过滤微弱振幅，仅当低音强度高于此值时发光。"),
        FloatPrefSpec("beatGlowDelayMs", 352.9f, 352.9f, 0f, 1000f, 50, "发光延迟补偿", "如果发光比声音早，可增加此延迟让光晕踩准鼓点。"),
        // ---- 歌词动画 ----
        FloatPrefSpec("verticalScrollSpeed", 0.5f, 0.5f, 0.1f, 1f, 8, "滚动速度", "歌词追焦时的纵向滚动速度。"),
        FloatPrefSpec("scaleAnimationSpeed", 0.5f, 0.5f, 0.1f, 1f, 8, "缩放速度", "当前行高亮时的缩放进入速度。"),
        FloatPrefSpec("activeLyricSizeRatio", 0.7f, 0.7f, 0.1f, 1f, 8, "居中放大", "控制视觉中心处当前歌词的强调程度。"),
        FloatPrefSpec("baseFontSizeRatio", 1.3f, 1.3f, 0.5f, 2f, 15, "所有字号", "统一缩放歌词字号，快速试不同观感。"),
        // 两端默认不同：手机 0.7 / 平板 0.5
        FloatPrefSpec("lineSpacingRatio", 0.7f, 0.5f, 0.5f, 3f, 25, "歌词行距", "让歌词排布更紧凑或更舒展。"),
        // ---- 逐字细节 ----
        // 两端默认不同：手机 0.3 / 平板 2.0
        FloatPrefSpec("yrcFloatSpeed", 0.3f, 2.0f, 0.1f, 2f, 18, "上浮速度", "控制逐字高亮向上浮动的响应速度。"),
        // 两端默认不同：手机 12 / 平板 3.92
        FloatPrefSpec("yrcFloatIntensity", 12f, 3.92f, 0f, 50f, 0, "上浮位移", "控制每个字高亮时抬升的幅度。"),
        FloatPrefSpec("wordTimingOffsetMs", 0f, 0f, -1000f, 1000f, 39, "逐字偏移", "整体提前或延后逐字时间点。"),
        // 两端默认不同：手机 0.4 / 平板 0.27
        FloatPrefSpec("wordScaleSpeed", 0.4f, 0.27f, 0.1f, 2f, 10, "缩放速度", "控制单字放大动画的速度。"),
        FloatPrefSpec("wordScaleSize", 1.0f, 1.0f, 1f, 2f, 13, "缩放大小", "控制单字高亮时的最大放大比例。")
    )

    /** 布尔开关清单。 */
    val bools: List<BoolPrefSpec> = listOf(
        BoolPrefSpec("enableWordByWord", true, true, "逐字歌词", "开启后歌词按单字跟随音乐高亮，关闭则整行显示。"),
        BoolPrefSpec("enableEdgeGlow", true, true, "启用发光", "开启或关闭屏幕边缘的随低音发光效果。")
    )

    /**
     * 几何微调（比例因子）。默认全为 1.0（不改变观感），作为「高级/几何微调」
     * 区里的比例旋钮：在 token 计算后做一次乘性调制，不引入任何像素 offset。
     */
    val geometry: List<FloatPrefSpec> = listOf(
        FloatPrefSpec("geoCoverScale", 1.0f, 1.0f, 0.6f, 1.4f, 20, "封面大小", "整体缩放封面（比例，1.0 = 默认）。"),
        FloatPrefSpec("geoControlScale", 1.0f, 1.0f, 0.7f, 1.4f, 20, "控件大小", "整体缩放播放键/动作键等控件（比例）。"),
        FloatPrefSpec("geoSpacingScale", 1.0f, 1.0f, 0.5f, 2.0f, 20, "间距密度", "整体缩放元素间距（比例，越大越疏）。"),
        FloatPrefSpec("geoPaddingScale", 1.0f, 1.0f, 0.5f, 2.0f, 20, "外边距", "整体缩放整页左右/上下边距（比例）。")
    )

    private val floatIndex: Map<String, FloatPrefSpec> by lazy { floats.associateBy { it.key } }
    private val boolIndex: Map<String, BoolPrefSpec> by lazy { bools.associateBy { it.key } }
    private val geometryIndex: Map<String, FloatPrefSpec> by lazy { geometry.associateBy { it.key } }

    /** 取某 key 在当前形态下的浮点默认值（含几何微调项）。 */
    fun floatDefault(key: String, ff: LyricsFormFactor): Float {
        val spec = floatIndex[key] ?: geometryIndex[key]
            ?: error("未在集中默认表中注册的滑块 key: $key")
        return if (ff == LyricsFormFactor.Phone) spec.phoneDefault else spec.tabletDefault
    }

    /** 取某 key 在当前形态下的布尔默认值。 */
    fun boolDefault(key: String, ff: LyricsFormFactor): Boolean {
        val spec = boolIndex[key] ?: error("未在集中默认表中注册的开关 key: $key")
        return if (ff == LyricsFormFactor.Phone) spec.phoneDefault else spec.tabletDefault
    }
}

/** 在 [ff] 形态下读取一个浮点滑块的 [key]（默认值取集中表）。 */
@Composable
fun rememberLyricsFloatPref(key: String, ff: LyricsFormFactor): MutableState<Float> =
    rememberFloatPreference(key, LyricsPrefDefaults.floatDefault(key, ff))

/** 在 [ff] 形态下读取一个布尔开关的 [key]（默认值取集中表）。 */
@Composable
fun rememberLyricsBoolPref(key: String, ff: LyricsFormFactor): MutableState<Boolean> =
    rememberBooleanPreference(key, LyricsPrefDefaults.boolDefault(key, ff))
