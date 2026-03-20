package com.example.mymusic.ui.screen.lyrics

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.example.mymusic.ui.theme.DarkBackground
import com.example.mymusic.ui.theme.DarkCard
import com.example.mymusic.ui.theme.NeteaseRed
import com.example.mymusic.ui.theme.TextPrimary
import com.example.mymusic.ui.theme.TextSecondary
import com.example.mymusic.ui.theme.TextTertiary
import com.example.mymusic.viewmodel.LyricsViewModel

data class SliderSettingItem(
    val label: String,
    val description: String,
    val value: Float,
    val onValueChange: (Float) -> Unit,
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int = 0
)

data class SliderSettingSection(
    val title: String,
    val description: String,
    val items: List<SliderSettingItem>
)

@Composable
fun SuiXinChangButton(
    currentSong: com.example.mymusic.data.model.Song?,
    lyricsViewModel: LyricsViewModel,
    isPhone: Boolean = false
) {
    val uiState by lyricsViewModel.state.collectAsState()
    val showSlider = uiState.suiXinChangSliderVisible
    fun setShowSlider(v: Boolean) {
        if (v) lyricsViewModel.showSuiXinChangSlider() else lyricsViewModel.hideSuiXinChangSlider()
    }

    var prevActive by remember { mutableStateOf(uiState.suiXinChangActive) }
    LaunchedEffect(uiState.suiXinChangActive) {
        if (uiState.suiXinChangActive && !prevActive) {
            lyricsViewModel.showSuiXinChangSlider()
        }
        prevActive = uiState.suiXinChangActive
    }

    LaunchedEffect(showSlider, uiState.suiXinChangVolume) {
        if (showSlider) {
            kotlinx.coroutines.delay(3000)
            lyricsViewModel.hideSuiXinChangSlider()
        }
    }

    val displaySlider = uiState.suiXinChangActive && showSlider
    val animatedVolume by animateFloatAsState(
        targetValue = uiState.suiXinChangVolume,
        animationSpec = tween(80, easing = LinearEasing),
        label = "volumeBar"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (displaySlider) 0f else 1f,
        animationSpec = tween(200),
        label = "iconAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (uiState.suiXinChangActive && !displaySlider) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "iconScale"
    )

    val buttonSizeDp = if (isPhone) 40.dp else 48.dp

    Box {
        IconButton(
            onClick = {
                if (currentSong != null) {
                    if (uiState.suiXinChangActive) setShowSlider(true) else lyricsViewModel.toggleSuiXinChang(currentSong)
                }
            },
            modifier = Modifier.size(buttonSizeDp)
        ) {
            if (uiState.suiXinChangLoading) {
                CircularProgressIndicator(
                    color = NeteaseRed,
                    modifier = Modifier
                        .size(16.dp)
                        .alpha(iconAlpha)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "随心唱",
                    tint = if (uiState.suiXinChangActive) {
                        NeteaseRed.copy(alpha = 0.85f)
                    } else {
                        TextSecondary.copy(alpha = 0.55f)
                    },
                    modifier = Modifier
                        .size(if (isPhone) 24.dp else 28.dp)
                        .alpha(iconAlpha)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }

        if (displaySlider) {
            Popup(
                alignment = Alignment.BottomCenter,
                offset = IntOffset(0, -(buttonSizeDp.value.toInt() + 8))
            ) {
                var targetAlpha by remember { mutableStateOf(0f) }
                var targetOffsetY by remember { mutableStateOf(20f) }
                LaunchedEffect(Unit) {
                    targetAlpha = 1f
                    targetOffsetY = 0f
                }
                val popupAlpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    animationSpec = tween(220),
                    label = "popupAlpha"
                )
                val popupOffsetY by animateFloatAsState(
                    targetValue = targetOffsetY,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "popupOffY"
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .width(28.dp)
                        .height(120.dp)
                        .graphicsLayer {
                            alpha = popupAlpha
                            translationY = popupOffsetY.dp.toPx()
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkCard.copy(alpha = 0.72f))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                val totalHeight = size.height.toFloat()
                                onValueChangeWithShowSlider(
                                    lyricsViewModel,
                                    (1f - (down.position.y / totalHeight)).coerceIn(0f, 1f)
                                ) { setShowSlider(true) }
                                var pointer = down
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val current = event.changes.firstOrNull { it.id == pointer.id }
                                    if (current != null && current.pressed) {
                                        pointer = current
                                        onValueChangeWithShowSlider(
                                            lyricsViewModel,
                                            (1f - (current.position.y / totalHeight)).coerceIn(0f, 1f)
                                        ) { setShowSlider(true) }
                                        current.consume()
                                    } else {
                                        break
                                    }
                                }
                            }
                        }
                ) {
                    val activeHeight = maxHeight * animatedVolume
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(activeHeight)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        NeteaseRed.copy(alpha = 0.6f),
                                        NeteaseRed.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

private fun onValueChangeWithShowSlider(
    lyricsViewModel: LyricsViewModel,
    value: Float,
    showSliderAction: () -> Unit
) {
    lyricsViewModel.setSuiXinChangVolume(value)
    showSliderAction()
}

@Composable
fun SettingSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    var showDialog by remember { mutableStateOf(false) }
    var tempValue by remember { mutableStateOf(value.toString()) }

    val displayText = formatSliderValue(label, value)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = displayText,
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable {
                        tempValue = value.toString()
                        showDialog = true
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                NeteaseRed.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.08f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = NeteaseRed.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                textAlign = TextAlign.Center
            )
        }

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                activeTrackColor = NeteaseRed,
                thumbColor = Color.White,
                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                activeTickColor = NeteaseRed,
                inactiveTickColor = Color.White.copy(alpha = 0.16f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatSliderEdgeValue(label, valueRange.start),
                color = TextTertiary,
                fontSize = 12.sp
            )
            Text(
                text = "点击数值可精确输入",
                color = TextTertiary,
                fontSize = 12.sp
            )
            Text(
                text = formatSliderEdgeValue(label, valueRange.endInclusive),
                color = TextTertiary,
                fontSize = 12.sp
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("设置 $label", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = tempValue,
                    onValueChange = { tempValue = it },
                    label = { Text("数值") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedLabelColor = NeteaseRed,
                        unfocusedLabelColor = TextSecondary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newValue = tempValue.toFloatOrNull()
                        if (newValue != null) onValueChange(newValue.coerceIn(valueRange))
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消", color = TextSecondary)
                }
            },
            containerColor = DarkBackground
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsBottomSheet(
    title: String,
    subtitle: String,
    isPhone: Boolean,
    sections: List<SliderSettingSection>,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = if (isPhone) 620.dp else 760.dp)
                .padding(horizontal = if (isPhone) 20.dp else 28.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = if (isPhone) 18.sp else 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextTertiary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                sections.forEach { section ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(DarkCard.copy(alpha = 0.72f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = section.title,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = section.description,
                                color = TextTertiary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        section.items.forEach { item ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = item.description,
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                                SettingSliderRow(
                                    label = item.label,
                                    value = item.value,
                                    onValueChange = item.onValueChange,
                                    valueRange = item.valueRange,
                                    steps = item.steps
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatSliderValue(label: String, value: Float): String {
    return when {
        label.contains("X") || label.contains("Y") -> String.format(java.util.Locale.US, "%.0f", value)
        label.contains("偏移") -> String.format(java.util.Locale.US, "%+.0fms", value)
        label.contains("宽") && !label.contains("偏移") -> String.format(java.util.Locale.US, "%.1fx", value)
        label.contains("大小") -> String.format(java.util.Locale.US, "%.1fx", value)
        else -> String.format(java.util.Locale.US, "%.1fx", value)
    }
}

private fun formatSliderEdgeValue(label: String, value: Float): String {
    return when {
        label.contains("X") || label.contains("Y") -> String.format(java.util.Locale.US, "%.0f", value)
        label.contains("偏移") -> String.format(java.util.Locale.US, "%+.0fms", value)
        else -> String.format(java.util.Locale.US, "%.1f", value)
    }
}
