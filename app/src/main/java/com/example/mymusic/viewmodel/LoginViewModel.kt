package com.example.mymusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.model.Profile
import com.example.mymusic.data.model.QrCreateData
import com.example.mymusic.data.network.ApiClient
import com.example.mymusic.data.repository.MusicRepository
import com.example.mymusic.data.repository.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoginTab { CAPTCHA, QR }

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val profile: Profile? = null,
    val errorMessage: String? = null,
    val phone: String = "",
    val captcha: String = "",
    val isSendingCode: Boolean = false,
    val codeSent: Boolean = false,
    val activeTab: LoginTab = LoginTab.CAPTCHA,
    val isAndroidMode: Boolean = false,
    val isTabletMode: Boolean = true, // 默认开启平板模式，因为验证过最稳
    // QR 状态
    val qrKey: String = "",
    val qrData: QrCreateData? = null,
    val qrStatusMessage: String = "等待扫码"
)

class LoginViewModel : ViewModel() {

    private val repository = MusicRepository()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var qrPollJob: Job? = null

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            val cookie = ApiClient.getCookie()
            val hasMusicU = cookie.contains("MUSIC_U", ignoreCase = true)
            
            println("DEBUG: checkLoginStatus cookie has MUSIC_U: $hasMusicU")
            
            if (hasMusicU) {
                // 有登录 Cookie，先假定登录有效，立即进入主界面
                _uiState.value = _uiState.value.copy(
                    isLoggedIn = true,
                    isLoading = false // 这里设为 false 让 Splash 尽快放行
                )
                
                // 后台静默验证
                try {
                    val result = repository.getLoginStatus()
                    if (result is Result.Success) {
                        val profile = result.data.data?.profile
                        if (profile != null) {
                            println("DEBUG Profile: Background login validation: SUCCESS for ${profile.nickname}")
                            _uiState.value = _uiState.value.copy(profile = profile)
                        } else {
                            // 暂时不要直接重置，因为某些接口（如平板/PC切换）可能导致 profile 短暂拿不到，但 MUSIC_U 其实还有效
                            println("DEBUG Profile: Background login validation: profile is null, but keeping session")
                            // _uiState.value = _uiState.value.copy(isLoggedIn = false)
                        }
                    } else {
                        // 网络异常，由于已经进来了，我们保留 isLoggedIn=true，让用户继续用（请求接口会再校验）
                        println("DEBUG: Background login validation: NETWORK ERROR, sticking with session")
                    }
                } catch (e: Exception) {
                    println("DEBUG: Background login validation: EXCEPTION ${e.message}")
                }
            } else {
                // 彻底没有 Cookie，跳转到登录页
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = false
                )
            }
        }
    }

    fun setPhone(value: String) {
        _uiState.value = _uiState.value.copy(phone = value, codeSent = false)
    }

    fun setCaptcha(value: String) {
        _uiState.value = _uiState.value.copy(captcha = value)
    }

    fun toggleAndroidMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAndroidMode = enabled, isTabletMode = false)
        if (enabled) ApiClient.currentIdentity = ApiClient.NetIdentity.ANDROID
        else ApiClient.currentIdentity = ApiClient.NetIdentity.PC
    }

    fun toggleTabletMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isTabletMode = enabled, isAndroidMode = false)
        if (enabled) ApiClient.currentIdentity = ApiClient.NetIdentity.TABLET
        else ApiClient.currentIdentity = ApiClient.NetIdentity.PC
    }

    fun setActiveTab(tab: LoginTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        if (tab == LoginTab.QR) {
            startQrCodeLogin()
        } else {
            qrPollJob?.cancel()
        }
    }

    fun sendCaptcha() {
        val phone = _uiState.value.phone.trim()
        if (phone.length != 11) {
            _uiState.value = _uiState.value.copy(errorMessage = "请输入11位手机号")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingCode = true, errorMessage = null)
            when (val result = repository.sendCaptcha(phone)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isSendingCode = false, codeSent = true)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingCode = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun loginByCaptcha() {
        val state = _uiState.value
        if (state.phone.isBlank() || state.captcha.isBlank()) {
            _uiState.value = state.copy(errorMessage = "手机号和验证码不能为空")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.loginByCaptcha(state.phone.trim(), state.captcha.trim())) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        profile = result.data.profile
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }

    // 启动游客模式
    fun loginAsGuest() {
        _uiState.value = _uiState.value.copy(isLoggedIn = true)
        // 游客模式不需要真正的登录请求，直接进入主界面
        // 实际使用时 ApiClient 会自动处理匿名 Token
    }

    fun startQrCodeLogin() {
        qrPollJob?.cancel()
        val state = _uiState.value
        _uiState.value = state.copy(
            qrStatusMessage = "正在获取二维码...",
            qrKey = "",
            qrData = null
        )
        
        viewModelScope.launch {
            val keyResult = if (state.isTabletMode) {
                repository.getQrKeyTablet()
            } else {
                repository.getQrKey(3) // 默认 APP
            }

            if (keyResult is Result.Success) {
                val key = keyResult.data
                _uiState.value = _uiState.value.copy(qrKey = key)
                
                val createResult = repository.createQrCode(key, if (state.isTabletMode) 2 else 3)
                if (createResult is Result.Success) {
                    _uiState.value = _uiState.value.copy(qrData = createResult.data, qrStatusMessage = "请使用网易云App扫码")
                    startQrPolling(key)
                } else {
                    _uiState.value = _uiState.value.copy(qrStatusMessage = "二维码生成失败")
                }
            } else {
                _uiState.value = _uiState.value.copy(qrStatusMessage = "获取Key失败")
            }
        }
    }

    private fun startQrPolling(key: String) {
        qrPollJob = viewModelScope.launch {
            while (true) {
                delay(3000)
                val state = _uiState.value // 实时获取最新状态
                val result = if (state.isTabletMode) {
                    repository.checkQrStatusTablet(key)
                } else {
                    repository.checkQrStatus(key, 3)
                }
                
                if (result is Result.Success) {
                    val code = result.data.code
                    println("DEBUG ViewModel: QR Poll result code=$code")
                    when (code) {
                        800 -> {
                            _uiState.value = _uiState.value.copy(qrStatusMessage = "二维码已过期，请点击刷新")
                            qrPollJob?.cancel()
                        }
                        801 -> { /* 等待扫码 */ }
                        802 -> {
                            _uiState.value = _uiState.value.copy(qrStatusMessage = "扫码成功，请在手机上确认")
                        }
                        803 -> {
                            println("DEBUG ViewModel: Login SUCCESS (code 803)")
                            _uiState.value = _uiState.value.copy(
                                qrStatusMessage = "登录成功",
                                isLoggedIn = true
                            )
                            qrPollJob?.cancel()
                        }
                    }
                } else {
                    println("DEBUG ViewModel: QR Poll ERROR result")
                }
            }
        }
    }

    fun stopQrPolling() {
        qrPollJob?.cancel()
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.value = LoginUiState()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        qrPollJob?.cancel()
    }
}
