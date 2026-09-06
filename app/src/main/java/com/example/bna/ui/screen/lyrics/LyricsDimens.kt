package com.example.bna.ui.screen.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 歌词播放页的全局尺寸令牌（单一来源）。
 *
 * 取代旧实现里「两套近乎重复的布局文件各自写死尺寸 + 三级缩放乘法
 * (scale * buttonSizeRatio * fitScale) + 内联魔法数」。所有共享控件
 * （播放三键、底部动作键、进度条、封面、标题）只从 [LocalLyricsDimens]
 * 读取尺寸，不再接收 isPhone / scale / ratio 等逐层参数。
 *
 * 参考 Salt Player：尺寸在一次约束测量后由 [computeLyricsDimens] 算出，
 * 再通过 [LocalLyricsDimens]（CompositionLocal）下发给整棵子树。
 */
@Immutable
data class LyricsDimens(
    val isTablet: Boolean,
    /** 基准到实际屏幕的线性缩放，仅在计算时使用一次，不再向子组件叠加。 */
    val uiScale: Float,
    // ---- 文本 ----
    val titleFont: TextUnit,
    val artistFont: TextUnit,
    val specFont: TextUnit,
    val smallFont: TextUnit,
    // ---- 播放控制三键 ----
    val sideButtonSize: Dp,
    val playButtonSize: Dp,
    val sideIconSize: Dp,
    val playIconSize: Dp,
    val playbackMinSpacing: Dp,
    // ---- 顶栏关闭钮 ----
    val closeButtonSize: Dp,
    val closeIconSize: Dp,
    // ---- 底部动作按钮 ----
    val actionButtonSize: Dp,
    val actionIconSize: Dp,
    // ---- 进度条 ----
    val progressTrackHeight: Dp,
    val progressTouchHeight: Dp,
    val progressTimeFont: TextUnit,
    // ---- 封面 ----
    val coverCornerRadius: Dp,
    // ---- 间距 / 外边距 ----
    val spacingS: Dp,
    val spacingM: Dp,
    val spacingL: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp
) {
    companion object {
        val PhoneBaseline = LyricsDimens(
            isTablet = false,
            uiScale = 1f,
            titleFont = 20.sp,
            artistFont = 13.sp,
            specFont = 12.sp,
            smallFont = 12.sp,
            sideButtonSize = 40.dp,
            playButtonSize = 64.dp,
            sideIconSize = 28.dp,
            playIconSize = 48.dp,
            playbackMinSpacing = 8.dp,
            closeButtonSize = 40.dp,
            closeIconSize = 32.dp,
            actionButtonSize = 40.dp,
            actionIconSize = 24.dp,
            progressTrackHeight = 2.dp,
            progressTouchHeight = 20.dp,
            progressTimeFont = 12.sp,
            coverCornerRadius = 22.dp,
            spacingS = 8.dp,
            spacingM = 16.dp,
            spacingL = 28.dp,
            horizontalPadding = 14.dp,
            verticalPadding = 24.dp
        )

        val TabletBaseline = LyricsDimens(
            isTablet = true,
            uiScale = 1f,
            titleFont = 24.sp,
            artistFont = 14.sp,
            specFont = 11.sp,
            smallFont = 12.sp,
            sideButtonSize = 48.dp,
            playButtonSize = 72.dp,
            sideIconSize = 32.dp,
            playIconSize = 56.dp,
            playbackMinSpacing = 8.dp,
            closeButtonSize = 44.dp,
            closeIconSize = 32.dp,
            actionButtonSize = 48.dp,
            actionIconSize = 28.dp,
            progressTrackHeight = 2.dp,
            progressTouchHeight = 20.dp,
            progressTimeFont = 12.sp,
            coverCornerRadius = 16.dp,
            spacingS = 8.dp,
            spacingM = 16.dp,
            spacingL = 28.dp,
            horizontalPadding = 32.dp,
            verticalPadding = 32.dp
        )
    }
}

private fun Dp.scale(s: Float): Dp = this * s
private fun TextUnit.scale(s: Float): TextUnit = this * s

/**
 * 由可用空间一次性求出全部尺寸令牌。
 *
 * - 宽 >= 600dp 判定为平板/分栏布局（与原 `screenWidthDp >= 600` 一致）。
 * - 手机取短边基准；平板按相对密度线性缩放。缩放只在这里发生一次，
 *   输出的是绝对 token，后续所有控件不再有任何 scale 叠加。
 */
fun computeLyricsDimens(maxWidth: Dp, maxHeight: Dp): LyricsDimens {
    val isTablet = maxWidth >= 600.dp
    val base = if (isTablet) LyricsDimens.TabletBaseline else LyricsDimens.PhoneBaseline
    val shortSide = minOf(maxWidth, maxHeight)
    val scale = if (isTablet) {
        // 平板以 780dp 左右为基准，控件只随大屏轻微放大
        (shortSide / 700.dp).coerceIn(0.8f, 1.25f)
    } else {
        // 手机以短边 420dp 为 1.0 基准
        (shortSide / 420.dp).coerceIn(0.8f, 1.2f)
    }

    fun fontSize(t: TextUnit): TextUnit = if (isTablet) t else t * scale

    return LyricsDimens(
        isTablet = isTablet,
        uiScale = scale,
        titleFont = fontSize(base.titleFont),
        artistFont = fontSize(base.artistFont),
        specFont = fontSize(base.specFont),
        smallFont = fontSize(base.smallFont),
        sideButtonSize = base.sideButtonSize.scale(scale),
        playButtonSize = base.playButtonSize.scale(scale),
        sideIconSize = base.sideIconSize.scale(scale),
        playIconSize = base.playIconSize.scale(scale),
        playbackMinSpacing = base.playbackMinSpacing,
        closeButtonSize = base.closeButtonSize.scale(scale),
        closeIconSize = base.closeIconSize.scale(scale),
        actionButtonSize = base.actionButtonSize.scale(scale),
        actionIconSize = base.actionIconSize.scale(scale),
        progressTrackHeight = base.progressTrackHeight,
        progressTouchHeight = base.progressTouchHeight,
        progressTimeFont = base.progressTimeFont,
        coverCornerRadius = base.coverCornerRadius.scale(scale),
        spacingS = base.spacingS.scale(scale),
        spacingM = base.spacingM.scale(scale),
        spacingL = base.spacingL.scale(scale),
        horizontalPadding = if (isTablet) {
            (maxWidth * 0.05f).coerceIn(base.horizontalPadding, 64.dp)
        } else base.horizontalPadding.scale(scale),
        verticalPadding = if (isTablet) {
            (maxHeight * 0.05f).coerceIn(16.dp, 48.dp)
        } else base.verticalPadding.scale(scale)
    )
}

/** 全歌词页范围内共享的尺寸令牌（由根布局提供，仿 salt 的 CompositionLocal 主题做法）。 */
val LocalLyricsDimens = staticCompositionLocalOf { LyricsDimens.PhoneBaseline }

/**
 * 几何微调（比例因子，默认全 1.0 = 不改变观感）。
 *
 * 「高级 / 几何微调」区的旋钮即这些因子：在 [computeLyricsDimens] 算出的
 * token 之上做**一次乘性调制**。全是比例、不引入任何像素 offset，换屏不炸。
 * 封面大小因子单独由外壳的 coverSize 计算点消费。
 */
@Immutable
data class LyricsGeometryTuning(
    /** 整体缩放控件（播放键/动作键/关闭钮/进度条热区）。 */
    val controlScale: Float = 1f,
    /** 整体缩放元素间距（S/M/L）。 */
    val spacingScale: Float = 1f,
    /** 整体缩放整页外边距。 */
    val paddingScale: Float = 1f
)

/** 在 [computeLyricsDimens] 结果上应用几何微调（仅一次乘性调制，无叠加链）。 */
fun LyricsDimens.applyGeometryTuning(t: LyricsGeometryTuning): LyricsDimens =
    if (t.controlScale == 1f && t.spacingScale == 1f && t.paddingScale == 1f) {
        this
    } else {
        this.copy(
            sideButtonSize = sideButtonSize * t.controlScale,
            playButtonSize = playButtonSize * t.controlScale,
            sideIconSize = sideIconSize * t.controlScale,
            playIconSize = playIconSize * t.controlScale,
            playbackMinSpacing = playbackMinSpacing * t.controlScale,
            closeButtonSize = closeButtonSize * t.controlScale,
            closeIconSize = closeIconSize * t.controlScale,
            actionButtonSize = actionButtonSize * t.controlScale,
            actionIconSize = actionIconSize * t.controlScale,
            progressTrackHeight = progressTrackHeight * t.controlScale,
            progressTouchHeight = progressTouchHeight * t.controlScale,
            spacingS = spacingS * t.spacingScale,
            spacingM = spacingM * t.spacingScale,
            spacingL = spacingL * t.spacingScale,
            horizontalPadding = horizontalPadding * t.paddingScale,
            verticalPadding = verticalPadding * t.paddingScale
        )
    }
