package com.example.bna.ui.screen.lyrics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit

/**
 * 歌词页「几何尺寸」的集中登记表（单一 schema / 元数据源）。
 *
 * 与效果滑块表 [LyricsPrefDefaults] 对应：效果滑块走 prefs，几何尺寸走
 * [LyricsDimens] token。这里把几何各维度的 key、分类、双档默认与语义
 * 集中登记一遍，作为：
 *   - agent「填表式固化 / 导出」几何默认的对照依据；
 *   - 将来把几何暴露成「比例旋钮」时的元数据来源（UI 主面板精简 + 高级收起）。
 *
 * **关键约束**：本表不持有运行逻辑，值始终从 [LyricsDimens.PhoneBaseline] /
 * [LyricsDimens.TabletBaseline] 读取，避免与 token 出现第二套值源而漂移。
 * 未来某维度若转为可调旋钮，再在 [computeLyricsDimens] 消费阶段叠加一次比例调制。
 */

/** 几何维度分类（用于设置界面分组 / 导出排序）。 */
enum class LyricsDimCategory {
    /** 封面：大小、圆角 */
    COVER,
    /** 外边距 / 整页留白 */
    EDGE,
    /** 元素间距（S/M/L 一套比例尺） */
    SPACING,
    /** 播放三键 / 动作键 / 关闭钮 */
    BUTTON,
    /** 顶栏 / 状态文字字号 */
    TEXT,
    /** 进度条 */
    PROGRESS
}

/** 将来是否适合暴露成「比例旋钮」做二次微调。 */
enum class LyricsDimTunable {
    /** 只随 token 缩放，不建议手调 */
    FIXED,
    /** 可暴露成比例旋钮（在 token 结果上做一次比例调制） */
    PROPORTIONAL
}

/** 几何尺寸条目：尺寸类（Dp）。 */
@Immutable
data class LyricsDimDpSpec(
    val key: String,
    val category: LyricsDimCategory,
    val tunable: LyricsDimTunable,
    val label: String,
    val description: String = "",
    val phone: Dp,
    val tablet: Dp
)

/** 几何尺寸条目：字号类（TextUnit）。 */
@Immutable
data class LyricsDimSpSpec(
    val key: String,
    val category: LyricsDimCategory,
    val tunable: LyricsDimTunable,
    val label: String,
    val description: String = "",
    val phone: TextUnit,
    val tablet: TextUnit
)

/** 几何尺寸集中登记表（值从 [LyricsDimens] baseline 读取，单一值源）。 */
object LyricsDimensRegistry {

    /** 尺寸（Dp）维度。 */
    val dps: List<LyricsDimDpSpec> by lazy {
        val p = LyricsDimens.PhoneBaseline
        val t = LyricsDimens.TabletBaseline
        listOf(
            // ---- 封面 ----
            LyricsDimDpSpec("coverCornerRadius", LyricsDimCategory.COVER, LyricsDimTunable.FIXED, "封面圆角", "封面四角圆角半径。", p.coverCornerRadius, t.coverCornerRadius),
            // ---- 外边距 ----
            LyricsDimDpSpec("horizontalPadding", LyricsDimCategory.EDGE, LyricsDimTunable.PROPORTIONAL, "左右边距", "整页左右外边距。", p.horizontalPadding, t.horizontalPadding),
            LyricsDimDpSpec("verticalPadding", LyricsDimCategory.EDGE, LyricsDimTunable.PROPORTIONAL, "上下边距", "整页上下外边距。", p.verticalPadding, t.verticalPadding),
            // ---- 间距 ----
            LyricsDimDpSpec("spacingS", LyricsDimCategory.SPACING, LyricsDimTunable.PROPORTIONAL, "间距S", "紧凑级间距（行内/小空隙）。", p.spacingS, t.spacingS),
            LyricsDimDpSpec("spacingM", LyricsDimCategory.SPACING, LyricsDimTunable.PROPORTIONAL, "间距M", "常用级间距。", p.spacingM, t.spacingM),
            LyricsDimDpSpec("spacingL", LyricsDimCategory.SPACING, LyricsDimTunable.PROPORTIONAL, "间距L", "区块级间距（列距/段距）。", p.spacingL, t.spacingL),
            // ---- 播放三键 ----
            LyricsDimDpSpec("sideButtonSize", LyricsDimCategory.BUTTON, LyricsDimTunable.PROPORTIONAL, "旁键尺寸", "上一首/下一首按钮。", p.sideButtonSize, t.sideButtonSize),
            LyricsDimDpSpec("playButtonSize", LyricsDimCategory.BUTTON, LyricsDimTunable.PROPORTIONAL, "播放键尺寸", "播放/暂停主按钮。", p.playButtonSize, t.playButtonSize),
            LyricsDimDpSpec("sideIconSize", LyricsDimCategory.BUTTON, LyricsDimTunable.FIXED, "旁键图标", "旁键内图标。", p.sideIconSize, t.sideIconSize),
            LyricsDimDpSpec("playIconSize", LyricsDimCategory.BUTTON, LyricsDimTunable.FIXED, "播放图标", "播放键内图标。", p.playIconSize, t.playIconSize),
            LyricsDimDpSpec("playbackMinSpacing", LyricsDimCategory.BUTTON, LyricsDimTunable.FIXED, "播放键最小间距", "三键间距下限。", p.playbackMinSpacing, t.playbackMinSpacing),
            LyricsDimDpSpec("closeButtonSize", LyricsDimCategory.BUTTON, LyricsDimTunable.FIXED, "关闭钮尺寸", "顶栏关闭按钮。", p.closeButtonSize, t.closeButtonSize),
            LyricsDimDpSpec("closeIconSize", LyricsDimCategory.BUTTON, LyricsDimTunable.FIXED, "关闭图标", "顶栏关闭图标。", p.closeIconSize, t.closeIconSize),
            // ---- 动作键 ----
            LyricsDimDpSpec("actionButtonSize", LyricsDimCategory.BUTTON, LyricsDimTunable.PROPORTIONAL, "动作键尺寸", "底部动作按钮。", p.actionButtonSize, t.actionButtonSize),
            LyricsDimDpSpec("actionIconSize", LyricsDimCategory.BUTTON, LyricsDimTunable.FIXED, "动作键图标", "底部动作按钮内图标。", p.actionIconSize, t.actionIconSize),
            // ---- 进度条 ----
            LyricsDimDpSpec("progressTrackHeight", LyricsDimCategory.PROGRESS, LyricsDimTunable.FIXED, "进度轨高", "进度条轨道高度。", p.progressTrackHeight, t.progressTrackHeight),
            LyricsDimDpSpec("progressTouchHeight", LyricsDimCategory.PROGRESS, LyricsDimTunable.FIXED, "进度触点高", "进度条触控热区高度。", p.progressTouchHeight, t.progressTouchHeight)
        )
    }

    /** 字号（Sp）维度。 */
    val sps: List<LyricsDimSpSpec> by lazy {
        val p = LyricsDimens.PhoneBaseline
        val t = LyricsDimens.TabletBaseline
        listOf(
            LyricsDimSpSpec("titleFont", LyricsDimCategory.TEXT, LyricsDimTunable.PROPORTIONAL, "歌名字号", "顶栏歌名字号。", p.titleFont, t.titleFont),
            LyricsDimSpSpec("artistFont", LyricsDimCategory.TEXT, LyricsDimTunable.FIXED, "歌手字号", "顶栏歌手段字号。", p.artistFont, t.artistFont),
            LyricsDimSpSpec("specFont", LyricsDimCategory.TEXT, LyricsDimTunable.FIXED, "音质文本字号", "AudioTrack 状态文字。", p.specFont, t.specFont),
            LyricsDimSpSpec("smallFont", LyricsDimCategory.TEXT, LyricsDimTunable.FIXED, "小字字号", "次要文字。", p.smallFont, t.smallFont),
            LyricsDimSpSpec("progressTimeFont", LyricsDimCategory.PROGRESS, LyricsDimTunable.FIXED, "进度时间字号", "进度条两侧时间文本。", p.progressTimeFont, t.progressTimeFont)
        )
    }

    private val dpIndex: Map<String, LyricsDimDpSpec> by lazy { dps.associateBy { it.key } }
    private val spIndex: Map<String, LyricsDimSpSpec> by lazy { sps.associateBy { it.key } }

    /** 查尺寸维度，key 不存在返回 null（供导出/工具遍历）。 */
    fun dp(key: String): LyricsDimDpSpec? = dpIndex[key]

    /** 查字号维度，key 不存在返回 null（供导出/工具遍历）。 */
    fun sp(key: String): LyricsDimSpSpec? = spIndex[key]

    /** 尺寸维度的手机/平板默认值（与 [LyricsDimens] 同源）。 */
    fun dpDefault(key: String, ff: LyricsFormFactor): Dp? =
        dp(key)?.let { if (ff == LyricsFormFactor.Phone) it.phone else it.tablet }

    /** 字号维度的手机/平板默认值（与 [LyricsDimens] 同源）。 */
    fun spDefault(key: String, ff: LyricsFormFactor): TextUnit? =
        sp(key)?.let { if (ff == LyricsFormFactor.Phone) it.phone else it.tablet }
}
