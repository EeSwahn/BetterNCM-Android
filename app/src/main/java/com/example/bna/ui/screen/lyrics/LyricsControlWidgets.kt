package com.example.bna.ui.screen.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.activity.compose.BackHandler
import com.example.bna.ui.theme.DarkBackground
import com.example.bna.ui.theme.DarkCard
import com.example.bna.ui.theme.NeteaseRed
import com.example.bna.ui.theme.TextPrimary
import com.example.bna.ui.theme.TextSecondary
import com.example.bna.ui.theme.TextTertiary
import com.example.bna.viewmodel.LyricsViewModel
import kotlinx.coroutines.launch

sealed class BaseSettingItem

data class SliderSettingItem(
    val label: String,
    val description: String,
    val value: Float,
    val onValueChange: (Float) -> Unit,
    val valueRange: ClosedFloatingPointRange<Float>,
    val steps: Int = 0
) : BaseSettingItem()

data class SwitchSettingItem(
    val label: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
) : BaseSettingItem()

data class SliderSettingSection(
    val title: String,
    val description: String,
    val items: List<BaseSettingItem>
)

@Composable
fun SuiXinChangButton(
    currentSong: com.example.bna.data.model.Song?,
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

/** 更矮、更精致的细滑杆：透明 Slider 负责输入与步进，视觉上只画细轨道+填充+圆点 */
@Composable
private fun CompactSettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0
) {
    val start = valueRange.start
    val end = valueRange.endInclusive
    val span = if (end > start) (end - start) else 1f
    val frac = (((value - start) / span).coerceIn(0f, 1f))
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
    ) {
        val trackWidth = maxWidth
        val thumb = 16.dp
        // 底轨
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.14f))
        )
        // 已填充部分
        Box(
            modifier = Modifier
                .fillMaxWidth(frac)
                .height(4.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(NeteaseRed)
        )
        // 滑块圆点
        Box(
            modifier = Modifier
                .size(thumb)
                .offset(x = (trackWidth - thumb) * frac)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
        )
        // 透明滑杆：承接拖动/步进，视觉上不显示
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxSize(),
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                thumbColor = Color.Transparent,
                disabledThumbColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent,
                disabledActiveTickColor = Color.Transparent,
                disabledInactiveTickColor = Color.Transparent
            )
        )
    }
}

@Composable
fun SettingSliderRow(
    label: String,
    description: String = "",
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
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
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
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

        if (description.isNotBlank()) {
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        CompactSettingsSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
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
    wordByWordEnabled: Boolean? = null,
    onWordByWordChange: (Boolean) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onDismiss() }

    // 自定义底部浮层：不用官方 ModalBottomSheet，避免滚动内容时被误收起。
    val density = LocalDensity.current
    var dragPx by remember { mutableStateOf(0f) }
    val dragScope = rememberCoroutineScope()
    val dismissPx = with(density) { 260.dp.toPx() }
    fun finishDrag() {
        dragScope.launch {
            if (dragPx >= dismissPx) {
                onDismiss()
            } else {
                val a = Animatable(dragPx)
                a.animateTo(0f, animationSpec = tween(200)) { dragPx = value }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩：点按关闭
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        )

        // 底部面板：顶部把手可跟手下拖收起；内容区自由滚动，绝不因滚动误收起
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = dragPx }
                .heightIn(max = if (isPhone) 620.dp else 760.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(DarkBackground)
                .navigationBarsPadding()
                .padding(horizontal = if (isPhone) 20.dp else 28.dp)
                .padding(bottom = 20.dp)
        ) {
            // 顶部把手
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dy -> dragPx = (dragPx + dy).coerceAtLeast(0f) },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
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
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (wordByWordEnabled != null) {
                    Row(
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "逐字歌词",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "开启后歌词按单字跟随音乐高亮，关闭则整行显示。",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                        Switch(
                            checked = wordByWordEnabled,
                            onCheckedChange = onWordByWordChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeteaseRed,
                                checkedTrackColor = NeteaseRed.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                sections.forEach { section ->
                    var expanded by androidx.compose.runtime.saveable.rememberSaveable(section.title) {
                        mutableStateOf(false)
                    }
                    val arrowRotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = tween(250),
                        label = "sectionArrow_${section.title}"
                    )
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) { expanded = !expanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
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
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (expanded) "收起" else "展开",
                                tint = TextSecondary,
                                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation }
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        section.items.forEach { item ->
                            when (item) {
                                is SliderSettingItem -> {
                                    SettingSliderRow(
                                        label = item.label,
                                        description = item.description,
                                        value = item.value,
                                        onValueChange = item.onValueChange,
                                        valueRange = item.valueRange,
                                        steps = item.steps
                                    )
                                }
                                is SwitchSettingItem -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = item.label,
                                                color = TextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = item.description,
                                                color = TextSecondary,
                                                fontSize = 12.sp,
                                                lineHeight = 18.sp
                                            )
                                        }
                                        Switch(
                                            checked = item.checked,
                                            onCheckedChange = item.onCheckedChange,
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NeteaseRed,
                                                checkedTrackColor = NeteaseRed.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        val file = exportLyricsSettings(context)
                        Toast.makeText(context, "已导出：${file.absolutePath}", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeteaseRed)
                ) {
                    Text("导出当前数值")
                }
            }
        }
    }
}

private fun formatSliderValue(label: String, value: Float): String {
    return when {
        label.contains("亮度") || label.contains("强度") -> String.format(java.util.Locale.US, "%.0f%%", value * 100)
        label.contains("频率") -> if (value < 0.01f) "关闭" else String.format(java.util.Locale.US, "%.1fHz", value)
        label.contains("X") || label.contains("Y") -> String.format(java.util.Locale.US, "%.0f", value)
        label.contains("偏移") -> String.format(java.util.Locale.US, "%+.0fms", value)
        label.contains("字号") -> String.format(java.util.Locale.US, "%.0fsp", value)
        label.contains("间距") -> String.format(java.util.Locale.US, "%.0fdp", value)
        label.contains("宽") && !label.contains("偏移") -> String.format(java.util.Locale.US, "%.1fx", value)
        label.contains("大小") -> String.format(java.util.Locale.US, "%.1fx", value)
        else -> String.format(java.util.Locale.US, "%.1fx", value)
    }
}

private fun formatSliderEdgeValue(label: String, value: Float): String {
    return when {
        label.contains("亮度") || label.contains("强度") -> String.format(java.util.Locale.US, "%.0f%%", value * 100)
        label.contains("频率") -> if (value < 0.01f) "关闭" else String.format(java.util.Locale.US, "%.1fHz", value)
        label.contains("X") || label.contains("Y") -> String.format(java.util.Locale.US, "%.0f", value)
        label.contains("偏移") -> String.format(java.util.Locale.US, "%+.0fms", value)
        label.contains("字号") -> String.format(java.util.Locale.US, "%.0fsp", value)
        label.contains("间距") -> String.format(java.util.Locale.US, "%.0f", value)
        else -> String.format(java.util.Locale.US, "%.1f", value)
    }
}
