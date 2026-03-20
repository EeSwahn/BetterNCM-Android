package com.example.mymusic.ui.screen.lyrics

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.mymusic.data.model.Song
import com.example.mymusic.ui.animation.*
import com.example.mymusic.ui.theme.*
import com.example.mymusic.viewmodel.LyricsViewModel
import com.example.mymusic.viewmodel.LyricsUiState

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
    var lineSpacingRatio by rememberFloatPreference("lineSpacingRatio", 0.7f)
    var showSettings by remember { mutableStateOf(false) }

    var headerOffsetX by rememberFloatPreference("headerOffsetX", 0f)
    var headerOffsetY by rememberFloatPreference("headerOffsetY", 0f)
    var coverOffsetX by rememberFloatPreference("coverOffsetX", 0f)
    var coverOffsetY by rememberFloatPreference("coverOffsetY", 0f)
    var audioSpecOffsetX by rememberFloatPreference("audioSpecOffsetX", 0f)
    var audioSpecOffsetY by rememberFloatPreference("audioSpecOffsetY", 0f)
    var playbackOffsetX by rememberFloatPreference("playbackOffsetX", 0f)
    var playbackOffsetY by rememberFloatPreference("playbackOffsetY", 0f)
    var bottomOffsetX by rememberFloatPreference("bottomOffsetX", 0f)
    var bottomOffsetY by rememberFloatPreference("bottomOffsetY", 0f)
    var lyricsPanelOffsetX by rememberFloatPreference("lyricsPanelOffsetX", 0f)
    var lyricsPanelOffsetY by rememberFloatPreference("lyricsPanelOffsetY", 0f)
    
    var progressBarOffsetX by rememberFloatPreference("progressBarOffsetX", 0f)
    var progressBarOffsetY by rememberFloatPreference("progressBarOffsetY", 0f)
    var progressBarWidthRatio by rememberFloatPreference("progressBarWidthRatio", 1.0f)

    var enableWordByWord by rememberBooleanPreference("enableWordByWord", true)
    var yrcFloatSpeed by rememberFloatPreference("yrcFloatSpeed", 2.0f)
    var yrcFloatIntensity by rememberFloatPreference("yrcFloatIntensity", 3.92f)
    var wordTimingOffsetMs by rememberFloatPreference("wordTimingOffsetMs", 0f)
    var wordScaleSpeed by rememberFloatPreference("wordScaleSpeed", 0.27f)
    var wordScaleSize by rememberFloatPreference("wordScaleSize", 1.0f)

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 64.dp, vertical = 56.dp),
        horizontalArrangement = Arrangement.spacedBy(80.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            val nonCoverHeight = 360.dp 
            val idealWidth = minOf(maxWidth * 0.95f, maxHeight - nonCoverHeight).coerceAtLeast(200.dp)

            Column(
                modifier = Modifier.width(idealWidth).fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().offset(x = headerOffsetX.dp, y = headerOffsetY.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = song.name, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start, maxLines = 1)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = song.artistNames, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Start, maxLines = 1)
                    }
                    Icon(Icons.Default.Podcasts, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Box(
                    modifier = Modifier.fillMaxWidth().offset(x = coverOffsetX.dp, y = coverOffsetY.dp).aspectRatio(1f).clip(RoundedCornerShape(8.dp))
                ) {
                    if (song.albumCoverUrl.isNotEmpty()) {
                        AsyncImage(model = song.albumCoverUrl + "?param=800y800", contentDescription = song.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(DarkCard), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeteaseRed, modifier = Modifier.size(80.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "AudioTrack    FLAC 16 bits    48 kHz", color = TextTertiary, fontSize = 10.sp, modifier = Modifier.fillMaxWidth().offset(x = audioSpecOffsetX.dp, y = audioSpecOffsetY.dp), textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                ProgressBarOnly(offsetX = progressBarOffsetX, offsetY = progressBarOffsetY, widthRatio = progressBarWidthRatio)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(modifier = Modifier.offset(x = playbackOffsetX.dp, y = playbackOffsetY.dp)) {
                    PlaybackButtonsOnly(isPhone = false)
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                BottomActionButtons(
                    isPhone = false,
                    modifier = Modifier.offset(x = bottomOffsetX.dp, y = bottomOffsetY.dp),
                    onSettingsClick = { showSettings = !showSettings },
                    enableWordByWord = enableWordByWord,
                    onWordByWordChange = { enableWordByWord = it }
                )
            }
        }

        Box(
            modifier = Modifier.weight(1.2f).fillMaxHeight()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth().offset(x = lyricsPanelOffsetX.dp, y = lyricsPanelOffsetY.dp)) {
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.End) {
                    SuiXinChangButton(currentSong = song, lyricsViewModel = lyricsViewModel, isPhone = false)
                }
            }
        }
    }

    if (showSettings) {
        LyricsSettingsBottomSheet(
            title = "歌词布局与动画",
            subtitle = "平板模式下把动画参数和布局校准拆成几组，避免一长串滑块直接铺在正文里。",
            isPhone = false,
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
                    title = "左侧布局校准",
                    description = "主要用于校准标题、封面、音质信息和底部控制的相对位置。",
                    items = listOf(
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
