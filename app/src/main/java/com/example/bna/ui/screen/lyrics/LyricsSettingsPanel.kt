package com.example.bna.ui.screen.lyrics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.bna.ui.theme.DarkBackground
import com.example.bna.ui.theme.DarkCard
import com.example.bna.ui.theme.NeteaseRed
import com.example.bna.ui.theme.TextPrimary
import com.example.bna.ui.theme.TextSecondary
import com.example.bna.ui.theme.TextTertiary

/**
 * 歌词设置：不可拖拽关闭的独立面板（Dialog）。
 *
 * 取代会「下滑误关」的 ModalBottomSheet：改为 Dialog + [BackHandler] 关闭，
 * 内部内容可安全滚动。纵向高度也更紧凑：每个滑块只占 标题行 + 滑竿 两行，
 * 去掉原先占高的一整行刻度/提示。
 *
 * 分层：主面板放常用开关与效果分组 [sections]；
 * 「高级 / 几何微调」折叠区 [advanced] 默认收起（由每档外壳传入几何旋钮）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsPanel(
    title: String,
    subtitle: String,
    isPhone: Boolean,
    sections: List<SliderSettingSection>,
    wordByWordEnabled: Boolean? = null,
    onWordByWordChange: (Boolean) -> Unit = {},
    advanced: SliderSettingSection? = null,
    onDismiss: () -> Unit
) {
    var advancedOpen by remember { mutableStateOf(false) }
    BackHandler(onBack = onDismiss)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isPhone) 12.dp else 24.dp, vertical = if (isPhone) 16.dp else 28.dp),
            color = DarkBackground,
            shape = RoundedCornerShape(26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isPhone) 16.dp else 24.dp)
                    .padding(top = 16.dp, bottom = 8.dp)
            ) {
                // 头部
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(title, color = TextPrimary, fontSize = if (isPhone) 18.sp else 20.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = TextTertiary, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "关闭", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 内容区（可安全滚动，不会触发面板关闭）
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    wordByWordEnabled?.let { enabled ->
                        WordByWordToggleRow(enabled, onWordByWordChange)
                    }

                    sections.forEach { section ->
                        SectionCard(section)
                    }

                    // 「高级 / 几何微调」折叠区
                    advanced?.let { geo ->
                        CollapsibleAdvanced(geo, advancedOpen) { advancedOpen = !advancedOpen }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WordByWordToggleRow(enabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("逐字歌词", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("开启后歌词按单字跟随音乐高亮。", color = TextTertiary, fontSize = 12.sp, lineHeight = 16.sp)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeteaseRed,
                checkedTrackColor = NeteaseRed.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SectionCard(section: SliderSettingSection) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard.copy(alpha = 0.72f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(section.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(section.description, color = TextTertiary, fontSize = 11.sp, lineHeight = 15.sp)
        }
        section.items.forEach { item ->
            when (item) {
                is SliderSettingItem -> {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (item.description.isNotBlank()) {
                            Text(item.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                        SettingSliderRow(
                            label = item.label,
                            value = item.value,
                            onValueChange = item.onValueChange,
                            valueRange = item.valueRange,
                            steps = item.steps
                        )
                    }
                }
                is SwitchSettingItem -> SwitchRow(item)
            }
        }
    }
}

@Composable
private fun SwitchRow(item: SwitchSettingItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (item.description.isNotBlank()) {
                Text(item.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
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

@Composable
private fun CollapsibleAdvanced(section: SliderSettingSection, open: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(section.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (open) "" else section.description,
                    color = TextTertiary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            Icon(
                imageVector = if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        AnimatedVisibility(
            visible = open,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                section.items.forEach { item ->
                    when (item) {
                        is SliderSettingItem -> {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (item.description.isNotBlank()) {
                                    Text(item.description, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                                }
                                SettingSliderRow(
                                    label = item.label,
                                    value = item.value,
                                    onValueChange = item.onValueChange,
                                    valueRange = item.valueRange,
                                    steps = item.steps
                                )
                            }
                        }
                        is SwitchSettingItem -> SwitchRow(item)
                    }
                }
            }
        }
    }
}

/**
 * 构建「高级 / 几何微调」区的内容（比例因子，1.0 = 默认）。
 * 手机与平板共用同一组几何旋钮，字段由两端外壳把各自的 geo* prefs 传入。
 */
fun geometryAdvancedSection(
    coverScale: Float, onCoverScale: (Float) -> Unit,
    controlScale: Float, onControlScale: (Float) -> Unit,
    spacingScale: Float, onSpacingScale: (Float) -> Unit,
    paddingScale: Float, onPaddingScale: (Float) -> Unit
): SliderSettingSection = SliderSettingSection(
    title = "高级 · 几何微调",
    description = "整体比例旋钮，1.0 = 默认观感。只做整体缩放，不产生像素偏移。",
    items = listOf(
        SliderSettingItem("封面大小", "整体缩放封面。", coverScale, onCoverScale, 0.6f..1.4f, 20),
        SliderSettingItem("控件大小", "播放键 / 动作键等控件整体缩放。", controlScale, onControlScale, 0.7f..1.4f, 20),
        SliderSettingItem("间距密度", "元素间距整体缩放，越大越疏。", spacingScale, onSpacingScale, 0.5f..2.0f, 20),
        SliderSettingItem("外边距", "整页左右 / 上下边距整体缩放。", paddingScale, onPaddingScale, 0.5f..2.0f, 20)
    )
)
