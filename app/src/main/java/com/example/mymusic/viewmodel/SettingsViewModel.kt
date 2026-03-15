package com.example.mymusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LyricsAnimationState(
    val enable3DEffect: Boolean = true,
    val enableFlowingLight: Boolean = true,
    val enableGlowEffect: Boolean = true,
    val reduceFlowingLightEffect: Boolean = false,
    val flowingLightMode: Int = 0,
    val lyricsViewTextSize: Int = 22,
    val lyricsViewTextAlignCenter: Boolean = true,
    val lyricsViewBlur: Boolean = true,
    val lyricsUI3FontWeight: Int = 700
)

class SettingsViewModel : ViewModel() {
    
    private val _animationState = MutableStateFlow(LyricsAnimationState())
    val animationState: StateFlow<LyricsAnimationState> = _animationState.asStateFlow()
    
    fun update3DEffect(enabled: Boolean) {
        _animationState.value = _animationState.value.copy(enable3DEffect = enabled)
    }
    
    fun updateFlowingLight(enabled: Boolean) {
        _animationState.value = _animationState.value.copy(enableFlowingLight = enabled)
    }
    
    fun updateGlowEffect(enabled: Boolean) {
        _animationState.value = _animationState.value.copy(enableGlowEffect = enabled)
    }
    
    fun updateReduceFlowingLightEffect(enabled: Boolean) {
        _animationState.value = _animationState.value.copy(reduceFlowingLightEffect = enabled)
    }
    
    fun updateFlowingLightMode(mode: Int) {
        _animationState.value = _animationState.value.copy(flowingLightMode = mode)
    }
    
    fun updateLyricsViewTextSize(size: Int) {
        _animationState.value = _animationState.value.copy(lyricsViewTextSize = size)
    }
    
    fun updateLyricsViewTextAlignCenter(center: Boolean) {
        _animationState.value = _animationState.value.copy(lyricsViewTextAlignCenter = center)
    }
    
    fun updateLyricsViewBlur(blur: Boolean) {
        _animationState.value = _animationState.value.copy(lyricsViewBlur = blur)
    }
    
    fun updateLyricsUI3FontWeight(weight: Int) {
        _animationState.value = _animationState.value.copy(lyricsUI3FontWeight = weight)
    }
    
    fun resetToDefaults() {
        _animationState.value = LyricsAnimationState()
    }
}
