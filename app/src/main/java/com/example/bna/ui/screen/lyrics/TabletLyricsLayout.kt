package com.example.bna.ui.screen.lyrics

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.bna.data.model.Song
import com.example.bna.ui.animation.*
import com.example.bna.ui.theme.*
import com.example.bna.viewmodel.LyricsViewModel
import com.example.bna.viewmodel.LyricsUiState

@Composable
fun TabletLyricsLayout(
    currentSong: Song?,
    lyricsState: LyricsUiState,
    onDismiss: () -> Unit,
    lyricsViewModel: LyricsViewModel,
    animationConfig: LyricsAnimationConfig
) {
    val song = currentSong ?: return
    var verticalScrollSpeed by rememberFloatPreference("verticalScrollSpeed", 0.5f)
    var scaleAnimationSpeed by rememberFloatPreference("scaleAnimationSpeed", 0.5f)
    var activeLyricSizeRatio by rememberFloatPreference("activeLyricSizeRatio", 0.7f)
    var baseFontSizeRatio by rememberFloatPreference("baseFontSizeRatio", 1.3f)
    var lineSpacingRatio by rememberFloatPreference("lineSpacingRatio", 0.5f)
    var showSettings by remember { mutableStateOf(false) }

    var headerOffsetX by rememberFloatPreference("headerOffsetX", 0f)
    var headerOffsetY by rememberFloatPreference("headerOffsetY", 35.966827f)
    var coverOffsetX by rememberFloatPreference("coverOffsetX", 0f)
    var coverOffsetY by rememberFloatPreference("coverOffsetY", 50.522354f)
    var audioSpecOffsetX by rememberFloatPreference("audioSpecOffsetX", 0f)
    var audioSpecOffsetY by rememberFloatPreference("audioSpecOffsetY", 58.030396f)
    var playbackOffsetX by rememberFloatPreference("playbackOffsetX", 0f)
    var playbackOffsetY by rememberFloatPreference("playbackOffsetY", 46.11606f)
    var bottomOffsetX by rememberFloatPreference("bottomOffsetX", 0f)
    var bottomOffsetY by rememberFloatPreference("bottomOffsetY", 40.814377f)
    var lyricsPanelOffsetX by rememberFloatPreference("lyricsPanelOffsetX", 0f)
    var lyricsPanelOffsetY by rememberFloatPreference("lyricsPanelOffsetY", 0f)
    
    var progressBarOffsetX by rememberFloatPreference("progressBarOffsetX", 0f)
    var progressBarOffsetY by rememberFloatPreference("progressBarOffsetY", 53.176422f)
    var progressBarWidthRatio by rememberFloatPreference("progressBarWidthRatio", 1.2444445f)

    var playbackButtonSizeRatio by rememberFloatPreference("playbackButtonSizeRatio", 0.6888889f)
    var playbackButtonSpacingDp by rememberFloatPreference("playbackButtonSpacingDp", 0f)
    var bottomButtonSizeRatio by rememberFloatPreference("bottomButtonSizeRatio", 0.6f)
    var bottomButtonSpacingDp by rememberFloatPreference("bottomButtonSpacingDp", 0f)
    var coverSizeRatio by rememberFloatPreference("coverSizeRatio", 1.5f)

    var enableWordByWord by rememberBooleanPreference("enableWordByWord", true)
    var yrcFloatSpeed by rememberFloatPreference("yrcFloatSpeed", 2.0f)
    var yrcFloatIntensity by rememberFloatPreference("yrcFloatIntensity", 3.92f)
    var wordTimingOffsetMs by rememberFloatPreference("wordTimingOffsetMs", 0f)
    var wordScaleSpeed by rememberFloatPreference("wordScaleSpeed", 0.27f)
    var wordScaleSize by rememberFloatPreference("wordScaleSize", 1.0f)

    // 小尺寸平板适配：所有外边距、列间距按屏幕尺寸等比收缩，限制在原设计值以内
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val outerHorizontalPadding = (screenWidth * 0.055f).coerceIn(16.dp, 64.dp)
    val outerVerticalPadding = (screenHeight * 0.06f).coerceIn(10.dp, 56.dp)
    val columnSpacing = (screenWidth * 0.07f).coerceIn(16.dp, 80.dp)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = outerHorizontalPadding, vertical = outerVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(columnSpacing)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            // 以约 560dp 的左栏内容高度为基准做等比收缩，小屏上所有元素都能完整落位
            val uiScale = (maxHeight / 560.dp).coerceIn(0.45f, 1f)
            val nonCoverHeight = 340.dp * uiScale
            val idealWidth = (minOf(maxWidth * 0.95f, maxHeight - nonCoverHeight).coerceAtLeast(88.dp) * coverSizeRatio)
                .coerceIn(48.dp, maxWidth * 0.98f)

            // 估算封面以外固定内容的高度（含安全余量）。
            // 封面调大时多出来的高度从元素间距里借用，间距按比例收缩，
            // 保证封面能真正变大、底部按钮也永远不会被挤出屏幕
            val estimatedFixedHeight = 60.dp * uiScale +          // 标题区
                16.dp +                                            // 音质文本
                44.dp +                                            // 进度条 + 时间
                76.dp * uiScale * playbackButtonSizeRatio +        // 播放按钮
                (52.dp * bottomButtonSizeRatio + 16.dp) * uiScale +// 底部按钮
                28.dp                                              // 安全余量
            val coverCapByHeight = (maxHeight - estimatedFixedHeight - 24.dp * uiScale).coerceAtLeast(48.dp)
            val coverEdge = minOf(idealWidth, coverCapByHeight)
            val spacerScale = ((maxHeight - estimatedFixedHeight - coverEdge) / (84.dp * uiScale)).coerceIn(0.2f, 1f)

            Column(
                modifier = Modifier.width(coverEdge).fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().offset(x = (headerOffsetX * uiScale).dp, y = (headerOffsetY * uiScale).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = song.name, color = TextPrimary, fontSize = (24f * uiScale).coerceAtLeast(14f).sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp * uiScale))
                        Text(text = song.artistNames, color = TextSecondary, fontSize = (14f * uiScale).coerceAtLeast(10f).sp, textAlign = TextAlign.Start, maxLines = 1)
                    }
                    Icon(Icons.Default.Podcasts, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp * uiScale))
                }
                
                Spacer(modifier = Modifier.height(28.dp * uiScale * spacerScale))

                // 封面吃掉的高度从元素间距里借用，列宽跟随封面实际边长，控件保持与封面同宽
                Box(
                    modifier = Modifier
                        .size(coverEdge)
                        .offset(x = (coverOffsetX * uiScale).dp, y = (coverOffsetY * uiScale).dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (song.albumCoverUrl.isNotEmpty()) {
                        AsyncImage(model = song.albumCoverUrl + "?param=800y800", contentDescription = song.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(DarkCard), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeteaseRed, modifier = Modifier.size(80.dp * uiScale))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp * uiScale * spacerScale))
                
                Text(text = "AudioTrack    FLAC 16 bits    48 kHz", color = TextTertiary, fontSize = (10f * uiScale).coerceAtLeast(8f).sp, modifier = Modifier.fillMaxWidth().offset(x = (audioSpecOffsetX * uiScale).dp, y = (audioSpecOffsetY * uiScale).dp), textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(12.dp * uiScale * spacerScale))
                
                ProgressBarOnly(offsetX = progressBarOffsetX * uiScale, offsetY = progressBarOffsetY * uiScale, widthRatio = progressBarWidthRatio)
                
                Spacer(modifier = Modifier.height(24.dp * uiScale * spacerScale))
                
                Box(modifier = Modifier.offset(x = (playbackOffsetX * uiScale).dp, y = (playbackOffsetY * uiScale).dp)) {
                    PlaybackButtonsOnly(
                        isPhone = false,
                        scale = uiScale,
                        buttonSizeRatio = playbackButtonSizeRatio,
                        buttonSpacingDp = playbackButtonSpacingDp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                BottomActionButtons(
                    isPhone = false,
                    modifier = Modifier.offset(x = (bottomOffsetX * uiScale).dp, y = (bottomOffsetY * uiScale).dp),
                    onSettingsClick = { showSettings = !showSettings },
                    scale = uiScale,
                    buttonSizeRatio = bottomButtonSizeRatio,
                    buttonSpacingDp = bottomButtonSpacingDp
                )
            }
        }

        Box(
            modifier = Modifier.weight(1.2f).fillMaxHeight()
        ) {
            // 歌词面板占满整个右侧区域，底部出现界限与顶部消失界限到屏幕边缘距离相等；
            // 随心唱按钮悬浮于右下角原位置，不再挤压歌词区
            Box(modifier = Modifier.fillMaxSize().offset(x = lyricsPanelOffsetX.dp, y = lyricsPanelOffsetY.dp)) {
                LyricsPanel(
                    lyricsState = lyricsState,
                    lyricsViewModel = lyricsViewModel,
                    onDismiss = onDismiss,
                    animationConfig = animationConfig,
                    verticalScrollSpeed = verticalScrollSpeed,
                    scaleAnimationSpeed = scaleAnimationSpeed,
                    activeLyricSizeRatio = activeLyricSizeRatio,
                    baseFontSizeRatio = baseFontSizeRatio,
                    lineSpacingRatio = lineSpacingRatio,
                    enableWordByWord = enableWordByWord,
                    yrcFloatSpeed = yrcFloatSpeed,
                    yrcFloatIntensity = yrcFloatIntensity,
                    wordTimingOffsetMs = wordTimingOffsetMs,
                    wordScaleSpeed = wordScaleSpeed,
                    wordScaleSize = wordScaleSize
                )
            }

            Row(
                modifier = Modifier.align(Alignment.BottomEnd).padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.End
            ) {
                SuiXinChangButton(currentSong = song, lyricsViewModel = lyricsViewModel, isPhone = false)
            }
        }
    }

    if (showSettings) {
        LyricsSettingsBottomSheet(
            title = "歌词布局与动画",
            subtitle = "平板模式下把动画参数和布局校准拆成几组，避免一长串滑块直接铺在正文里。",
            isPhone = false,
            wordByWordEnabled = enableWordByWord,
            onWordByWordChange = { enableWordByWord = it },
            sections = listOf(
                SliderSettingSection(
                    title = "歌词动画",
                    description = "控制滚动、缩放和基础版式，影响主歌词区的整体节奏。",
                    items = listOf(
                        SliderSettingItem("滚动速度", "歌词追焦时的纵向滚动速度。", verticalScrollSpeed, { verticalScrollSpeed = it }, 0.1f..1.0f, 8),
                        SliderSettingItem("缩放速度", "当前行高亮时的缩放进入速度。", scaleAnimationSpeed, { scaleAnimationSpeed = it }, 0.1f..1.0f, 8),
                        SliderSettingItem("居中放大", "控制视觉中心处当前歌词的强调程度。", activeLyricSizeRatio, { activeLyricSizeRatio = it }, 0.1f..1.0f, 8),
                        SliderSettingItem("所有字号", "统一缩放歌词字号，快速试不同观感。", baseFontSizeRatio, { baseFontSizeRatio = it }, 0.5f..2.0f, 15),
                        SliderSettingItem("歌词行距", "让歌词排布更紧凑或更舒展。", lineSpacingRatio, { lineSpacingRatio = it }, 0.5f..3.0f, 25)
                    )
                ),
                SliderSettingSection(
                    title = "逐字细节",
                    description = "逐字歌词开启后，用这些参数做时序和动效校准。",
                    items = listOf(
                        SliderSettingItem("上浮速度", "控制逐字高亮向上浮动的响应速度。", yrcFloatSpeed, { yrcFloatSpeed = it }, 0.1f..2.0f, 18),
                        SliderSettingItem("上浮位移", "控制每个字高亮时抬升的幅度。", yrcFloatIntensity, { yrcFloatIntensity = it }, 0f..50f, 0),
                        SliderSettingItem("逐字偏移", "整体提前或延后逐字时间点。", wordTimingOffsetMs, { wordTimingOffsetMs = it }, -1000f..1000f, 39),
                        SliderSettingItem("缩放速度", "控制单字放大动画的速度。", wordScaleSpeed, { wordScaleSpeed = it }, 0.1f..2.0f, 10),
                        SliderSettingItem("缩放大小", "控制单字高亮时的最大放大比例。", wordScaleSize, { wordScaleSize = it }, 1.0f..2.0f, 13)
                    )
                ),
                SliderSettingSection(
                    title = "按钮调节",
                    description = "调整播放控制和底部动作按钮的大小与间距，间距为 0 时按布局自动分配。",
                    items = listOf(
                        SliderSettingItem("播放按钮大小", "整体缩放上一首、播放、下一首三个按钮。", playbackButtonSizeRatio, { playbackButtonSizeRatio = it }, 0.6f..1.4f, 8),
                        SliderSettingItem("播放按钮间距", "三个播放按钮之间的左右间距，0 表示按屏幕自适应。", playbackButtonSpacingDp, { playbackButtonSpacingDp = it }, 0f..60f, 12),
                        SliderSettingItem("底部按钮大小", "整体缩放底部一排动作按钮。", bottomButtonSizeRatio, { bottomButtonSizeRatio = it }, 0.6f..1.4f, 8),
                        SliderSettingItem("底部按钮间距", "底部按钮之间的左右间距，0 表示自动均分整行。", bottomButtonSpacingDp, { bottomButtonSpacingDp = it }, 0f..48f, 12)
                    )
                ),
                SliderSettingSection(
                    title = "左侧布局校准",
                    description = "主要用于校准标题、封面、音质信息和底部控制的相对位置。",
                    items = listOf(
                        SliderSettingItem("封面大小", "整体缩放左侧封面，其他元素位置保持不变。", coverSizeRatio, { coverSizeRatio = it }, 0.5f..1.5f, 10),
                        SliderSettingItem("标题X", "微调标题区的水平位置。", headerOffsetX, { headerOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("标题Y", "微调标题区的垂直位置。", headerOffsetY, { headerOffsetY = it }, -200f..200f, 0),
                        SliderSettingItem("封面X", "微调封面的水平位置。", coverOffsetX, { coverOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("封面Y", "微调封面的垂直位置。", coverOffsetY, { coverOffsetY = it }, -200f..200f, 0),
                        SliderSettingItem("音质X", "微调音质文本的水平位置。", audioSpecOffsetX, { audioSpecOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("音质Y", "微调音质文本的垂直位置。", audioSpecOffsetY, { audioSpecOffsetY = it }, -200f..200f, 0),
                        SliderSettingItem("控制X", "微调播放控制区的水平位置。", playbackOffsetX, { playbackOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("控制Y", "微调播放控制区的垂直位置。", playbackOffsetY, { playbackOffsetY = it }, -200f..200f, 0),
                        SliderSettingItem("底部X", "微调底部动作按钮的水平位置。", bottomOffsetX, { bottomOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("底部Y", "微调底部动作按钮的垂直位置。", bottomOffsetY, { bottomOffsetY = it }, -200f..200f, 0)
                    )
                ),
                SliderSettingSection(
                    title = "右侧布局校准",
                    description = "专门调整歌词区和进度条在平板大屏里的落位。",
                    items = listOf(
                        SliderSettingItem("歌词X", "微调右侧歌词面板的水平位置。", lyricsPanelOffsetX, { lyricsPanelOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("歌词Y", "微调右侧歌词面板的垂直位置。", lyricsPanelOffsetY, { lyricsPanelOffsetY = it }, -200f..200f, 0),
                        SliderSettingItem("进度条X", "微调进度条的水平位置。", progressBarOffsetX, { progressBarOffsetX = it }, -200f..200f, 0),
                        SliderSettingItem("进度条Y", "微调进度条的垂直位置。", progressBarOffsetY, { progressBarOffsetY = it }, -200f..200f, 0),
                        SliderSettingItem("进度条宽", "缩放进度条宽度，匹配不同平板比例。", progressBarWidthRatio, { progressBarWidthRatio = it }, 0.3f..2.0f, 17)
                    )
                )
            ),
            onDismiss = { showSettings = false }
        )
    }
}
