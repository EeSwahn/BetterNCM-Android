package com.example.bna.ui.animation

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset

/**
 * 椒盐音乐（Salt Player）动效规格表 —— 逆向自 v11.0.4。
 *
 * ## 逆向依据（全部为【观测】，来自反编译源码）
 *
 * salt 把动效规格定义成一个**枚举** `androidx.core.a61`（六个成员即六种命名动效）：
 * ```
 * DefaultSpatial, FastSpatial, SlowSpatial,      // "空间"曲线（有回弹）
 * DefaultEffects, FastEffects, SlowEffects       // "效果"曲线（无回弹）
 * ```
 * 由 `androidx.core.u44.m6380(a61, composer)` 解析到 `androidx.core.y51`
 * （实现类即 Material3 的 `StandardMotionSchemeImpl`）。
 *
 * `y51.java` L11-26 给出实际数值（构造器 `androidx.core.g33(dampingRatio, stiffness, visibilityThreshold)`，
 * 经 `t54.m6031(f=1.0f, f2=1500.0f, obj=null)` 生成）：
 *
 * | salt 枚举        | dampingRatio | stiffness | M3 对应名            |
 * |------------------|--------------|-----------|----------------------|
 * | DefaultSpatial   | 0.9f         | 700.0f    | defaultSpatialSpec   |
 * | FastSpatial      | 0.9f         | 1400.0f   | fastSpatialSpec      |
 * | SlowSpatial      | 0.9f         | 300.0f    | slowSpatialSpec      |
 * | DefaultEffects   | 1.0f         | 1600.0f   | defaultEffectsSpec   |
 * | FastEffects      | 1.0f         | 3800.0f   | fastEffectsSpec      |
 * | SlowEffects      | 1.0f         | 800.0f    | slowEffectsSpec      |
 *
 * 【观测】"空间"类 `dampingRatio = 0.9f`（< 1，有轻微过冲/回弹）；
 * "效果"类 `dampingRatio = 1.0f`（临界阻尼，不过冲，用于淡入淡出/透明度类）。
 *
 * ## 用法
 * 页面切换用 [pageSwitch]（对齐 salt `g52.java:28` 的 Crossfade 调用，
 * 它传的是 `a61.DefaultEffects`）；位移/缩放等"空间"动效用 [spatial]；
 * 透明度/淡入淡出等"效果"动效用 [effects]。
 *
 * ## 注意
 * Compose 的 `spring(dampingRatio, stiffness, visibilityThreshold)`
 * 第 3 个参数是 **visibilityThreshold 而非 mass**（已用 `javap` 对
 * animation-core 1.9.2 核对）。本项目不需要自定义阈值，故不传。
 * 本文件用命名参数调用，避免落到错误重载上。
 */
object SaltMotion {

    // ────────────────────────── "空间"曲线（有回弹）──────────────────────────

    /** salt `a61.DefaultSpatial`：dampingRatio 0.9f / stiffness 700f。默认位移、缩放。 */
    val DefaultSpatial: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.9f, stiffness = 700f)

    /** salt `a61.FastSpatial`：dampingRatio 0.9f / stiffness 1400f。小元素快速反馈（选中态、图标）。 */
    val FastSpatial: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.9f, stiffness = 1400f)

    /** salt `a61.SlowSpatial`：dampingRatio 0.9f / stiffness 300f。大块内容、抽屉等慢速位移。 */
    val SlowSpatial: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 0.9f, stiffness = 300f)

    // ────────────────────────── "效果"曲线（无回弹）──────────────────────────

    /** salt `a61.DefaultEffects`：dampingRatio 1.0f / stiffness 1600f。★页面 Crossfade 用的就是它。 */
    val DefaultEffects: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)

    /** salt `a61.FastEffects`：dampingRatio 1.0f / stiffness 3800f。淡入淡出、颜色过渡。 */
    val FastEffects: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 1.0f, stiffness = 3800f)

    /** salt `a61.SlowEffects`：dampingRatio 1.0f / stiffness 800f。大面淡入淡出。 */
    val SlowEffects: FiniteAnimationSpec<Float> =
        spring(dampingRatio = 1.0f, stiffness = 800f)

    // ────────────────────────────── 别名（语义化）──────────────────────────────

    /**
     * 页面/栏目切换动画规格。
     *
     * 【观测】salt `androidx.core.g52.java:28`：
     * `aq.m468(bool, null, u44.m6380(a61.DefaultEffects, composer), null, content)`
     * → 即 Crossfade + DefaultEffects。
     */
    val pageSwitch: FiniteAnimationSpec<Float> = DefaultEffects

    /** 列表项出现/重排（LazyColumn `animateItem`）。用 FastSpatial 保持轻快。 */
    val listItem: FiniteAnimationSpec<Float> = FastSpatial

    /** 列表项位置动画（IntOffset，用于 `animateItem` 的 placementSpec）。 */
    val listItemPlacement: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.9f, stiffness = 1400f)

    // ───────────── 位置类（IntOffset）：滑动入场/退场需要 IntOffset 规格 ─────────────
    // 注意 `slideInVertically(animationSpec = ...)` 的参数类型是 FiniteAnimationSpec<IntOffset>，
    // 传 <Float> 会编译失败，故这里单独提供一份等价规格。

    /** 位置位移：慢速"空间"曲线（对应 salt `a61.SlowSpatial`）。 */
    val SlowSpatialOffset: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.9f, stiffness = 300f)

    /** 位置位移：快速"空间"曲线（对应 salt `a61.FastSpatial`）。 */
    val FastSpatialOffset: FiniteAnimationSpec<IntOffset> =
        spring(dampingRatio = 0.9f, stiffness = 1400f)

    // ────────────────── Dp 类：`animateDpAsState` 需要 FiniteAnimationSpec<Dp> ──────────────────

    /** 尺寸/内边距动画：快速"空间"曲线（对应 salt `a61.FastSpatial`）。 */
    val FastSpatialDp: FiniteAnimationSpec<Dp> =
        spring(dampingRatio = 0.9f, stiffness = 1400f)

    /** 尺寸/内边距动画：慢速"空间"曲线（对应 salt `a61.SlowSpatial`）。 */
    val SlowSpatialDp: FiniteAnimationSpec<Dp> =
        spring(dampingRatio = 0.9f, stiffness = 300f)

    // ─────────────── Color 类：`animateColorAsState` 需要 FiniteAnimationSpec<Color> ───────────────

    /**
     * 颜色过渡（选中态背景、文字色）：
     * salt 把"效果"类用于透明度/淡入淡出，此处取 [FastEffects] 的数值。
     */
    val FastEffectsColor: FiniteAnimationSpec<Color> =
        spring(dampingRatio = 1.0f, stiffness = 3800f)

    /** 颜色过渡：默认"效果"曲线（对应 salt `a61.DefaultEffects`）。 */
    val DefaultEffectsColor: FiniteAnimationSpec<Color> =
        spring(dampingRatio = 1.0f, stiffness = 1600f)
}
