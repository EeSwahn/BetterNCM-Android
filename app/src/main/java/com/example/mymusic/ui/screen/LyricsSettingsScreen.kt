package com.example.mymusic.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mymusic.ui.theme.DarkBackground
import com.example.mymusic.ui.theme.DarkCard
import com.example.mymusic.ui.theme.NeteaseRed
import com.example.mymusic.ui.theme.TextPrimary
import com.example.mymusic.ui.theme.TextSecondary
import com.example.mymusic.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val animationState by settingsViewModel.animationState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "歌词动画设置",
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(title = "3D 效果") {
                SettingSwitch(
                    title = "启用 3D 立体效果",
                    description = "为歌词添加 3D 立体视觉效果",
                    checked = animationState.enable3DEffect,
                    onCheckedChange = { settingsViewModel.update3DEffect(it) }
                )
            }
            
            SettingsCard(title = "流光动画") {
                SettingSwitch(
                    title = "启用流光效果",
                    description = "在歌词上添加流动的光影效果",
                    checked = animationState.enableFlowingLight,
                    onCheckedChange = { settingsViewModel.updateFlowingLight(it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingSwitch(
                    title = "减弱流光效果",
                    description = "降低流光效果的强度",
                    checked = animationState.reduceFlowingLightEffect,
                    onCheckedChange = { settingsViewModel.updateReduceFlowingLightEffect(it) }
                )
            }
            
            SettingsCard(title = "发光效果") {
                SettingSwitch(
                    title = "启用发光效果",
                    description = "为当前歌词添加发光效果",
                    checked = animationState.enableGlowEffect,
                    onCheckedChange = { settingsViewModel.updateGlowEffect(it) }
                )
            }
            
            SettingsCard(title = "歌词显示") {
                SettingSwitch(
                    title = "歌词居中对齐",
                    description = "将歌词居中对齐显示",
                    checked = animationState.lyricsViewTextAlignCenter,
                    onCheckedChange = { settingsViewModel.updateLyricsViewTextAlignCenter(it) }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                SettingSwitch(
                    title = "背景模糊",
                    description = "为歌词背景添加模糊效果",
                    checked = animationState.lyricsViewBlur,
                    onCheckedChange = { settingsViewModel.updateLyricsViewBlur(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { settingsViewModel.resetToDefaults() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkCard,
                    contentColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "恢复默认设置",
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            content()
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 16.sp
            )
            
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeteaseRed,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = DarkCard
            )
        )
    }
}
