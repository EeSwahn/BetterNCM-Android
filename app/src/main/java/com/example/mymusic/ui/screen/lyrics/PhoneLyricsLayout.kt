package com.example.mymusic.ui.screen.lyrics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
fun PhoneLyricsLayout(
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
    var enableWordByWord by rememberBooleanPreference("enableWordByWord", true)
    var yrcFloatSpeed by rememberFloatPreference("yrcFloatSpeed", 1.0f)
    var yrcFloatIntensity by rememberFloatPreference("yrcFloatIntensity", 12f)
    var wordTimingOffsetMs by rememberFloatPreference("wordTimingOffsetMs", 0f)
    var wordScaleSpeed by rememberFloatPreference("wordScaleSpeed", 1.0f)
    var wordScaleSize by rememberFloatPreference("wordScaleSize", 1.3f)
    var showSettings by remember { mutableStateOf(false) }

    var showLyrics by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 24.dp, bottom = 24.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "关闭", tint = TextPrimary, modifier = Modifier.size(32.dp))
            }

            AnimatedVisibility(visible = showLyrics, enter = fadeIn() + slideInHorizontally(), exit = fadeOut() + slideOutHorizontally()) {
                Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    Text(text = song.name, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(text = song.artistNames, color = TextSecondary, fontSize = 14.sp, maxLines = 1)
                }
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { showLyrics = !showLyrics })
        ) {
            AnimatedContent(targetState = showLyrics, transitionSpec = { fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(500)) }, label = "lyrics_content_toggle") { displayLyrics ->
                if (displayLyrics) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            LyricsPanel(
                                lyricsState = lyricsState, lyricsViewModel = lyricsViewModel, onDismiss = onDismiss, isPhone = true, animationConfig = animationConfig, verticalScrollSpeed = verticalScrollSpeed, scaleAnimationSpeed = scaleAnimationSpeed, activeLyricSizeRatio = activeLyricSizeRatio, baseFontSizeRatio = baseFontSizeRatio, lineSpacingRatio = lineSpacingRatio, enableWordByWord = enableWordByWord, yrcFloatSpeed = yrcFloatSpeed, yrcFloatIntensity = yrcFloatIntensity, wordTimingOffsetMs = wordTimingOffsetMs, wordScaleSpeed = wordScaleSpeed, wordScaleSize = wordScaleSize
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.End) {
                            SuiXinChangButton(currentSong = song, lyricsViewModel = lyricsViewModel, isPhone = true)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(text = song.name, color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 2)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = song.artistNames, color = TextSecondary, fontSize = 18.sp, textAlign = TextAlign.Center, maxLines = 1)
                        Spacer(modifier = Modifier.height(48.dp))
                        Box(modifier = Modifier.fillMaxWidth(0.9f).aspectRatio(1f).clip(RoundedCornerShape(12.dp)).shadow(elevation = 24.dp, shape = RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = 0.6f))) {
                            if (song.albumCoverUrl.isNotEmpty()) {
                                AsyncImage(model = song.albumCoverUrl + "?param=800y800", contentDescription = song.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Box(modifier = Modifier.fillMaxSize().background(DarkCard), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = NeteaseRed, modifier = Modifier.size(80.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(text = "AudioTrack    FLAC 16 bits    48 kHz", color = TextTertiary, fontSize = 12.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        AnimatedVisibility(visible = showSettings) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SettingSliderRow("滚动速度", verticalScrollSpeed, { verticalScrollSpeed = it }, 0.1f..1.0f, 8)
                SettingSliderRow("缩放速度", scaleAnimationSpeed, { scaleAnimationSpeed = it }, 0.1f..1.0f, 8)
                SettingSliderRow("居中放大", activeLyricSizeRatio, { activeLyricSizeRatio = it }, 0.1f..1.0f, 8)
                SettingSliderRow("所有字号", baseFontSizeRatio, { baseFontSizeRatio = it }, 0.5f..2.0f, 15)
                SettingSliderRow("歌词行距", lineSpacingRatio, { lineSpacingRatio = it }, 0.5f..3.0f, 25)
                SettingSliderRow("上浮速度", yrcFloatSpeed, { yrcFloatSpeed = it }, 0.1f..2.0f, 18)
                SettingSliderRow("上浮位移", yrcFloatIntensity, { yrcFloatIntensity = it }, 0f..50f, 0)
                SettingSliderRow("逐字偏移", wordTimingOffsetMs, { wordTimingOffsetMs = it }, -1000f..1000f, 39)
                SettingSliderRow("缩放速度", wordScaleSpeed, { wordScaleSpeed = it }, 0.1f..2.0f, 10)
                SettingSliderRow("缩放大小", wordScaleSize, { wordScaleSize = it }, 1.0f..2.0f, 13)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        PlaybackControls(isPhone = true)
        Spacer(modifier = Modifier.height(16.dp))
        BottomActionButtons(isPhone = true, onSettingsClick = { showSettings = !showSettings }, enableWordByWord = enableWordByWord, onWordByWordChange = { enableWordByWord = it })
    }
}
