package com.example.mymusic.ui.screen

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mymusic.ui.theme.*
import com.example.mymusic.viewmodel.LoginTab
import com.example.mymusic.viewmodel.LoginViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    loginViewModel: LoginViewModel = viewModel()
) {
    val uiState by loginViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A0000),
                        Color(0xFF0D0D0D),
                        Color(0xFF0D0D0D)
                    )
                )
            )
    ) {
        // 背景装饰圆
        BackgroundDecoration()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            // Logo 区域
            LogoSection()

            Spacer(modifier = Modifier.height(48.dp))

            // Tab 切换
            LoginTabRow(
                activeTab = uiState.activeTab,
                onTabChange = loginViewModel::setActiveTab
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 登录模式选择 (身份模拟)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(12.dp)
            ) {
                // 安卓模式开关
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = "安卓模拟模式",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.isAndroidMode) NeteaseRed else TextSecondary
                    )
                    Switch(
                        checked = uiState.isAndroidMode,
                        onCheckedChange = { 
                            loginViewModel.toggleAndroidMode(it)
                            if (uiState.activeTab == LoginTab.QR) loginViewModel.startQrCodeLogin()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeteaseRed
                        )
                    )
                }

                // 平板模式开关
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text(
                        text = "平板模拟模式 (推荐)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.isTabletMode) NeteaseRed else TextSecondary
                    )
                    Switch(
                        checked = uiState.isTabletMode,
                        onCheckedChange = { 
                            loginViewModel.toggleTabletMode(it)
                            if (uiState.activeTab == LoginTab.QR) loginViewModel.startQrCodeLogin()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = NeteaseRed
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 登录表单区域
            AnimatedContent(
                targetState = uiState.activeTab,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState == LoginTab.CAPTCHA) -it else it }
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { if (targetState == LoginTab.CAPTCHA) it else -it }
                    )
                },
                label = "login_tab_transition"
            ) { tab ->
                when (tab) {
                    LoginTab.CAPTCHA -> CaptchaLoginForm(
                        phone = uiState.phone,
                        captcha = uiState.captcha,
                        isLoading = uiState.isLoading,
                        isSendingCode = uiState.isSendingCode,
                        codeSent = uiState.codeSent,
                        onPhoneChange = loginViewModel::setPhone,
                        onCaptchaChange = loginViewModel::setCaptcha,
                        onSendCode = loginViewModel::sendCaptcha,
                        onLogin = loginViewModel::loginByCaptcha
                    )
                    else -> QrLoginForm(
                        isLoading = uiState.isLoading,
                        qrData = uiState.qrData,
                        statusMessage = uiState.qrStatusMessage
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 游客登录入口
            if (uiState.activeTab == LoginTab.CAPTCHA) {
                TextButton(
                    onClick = { loginViewModel.loginAsGuest() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text("暂不登录，以游客身份体验 >", color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 错误提示
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { msg ->
                    ErrorBanner(
                        message = msg,
                        onDismiss = loginViewModel::clearError
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 底部提示
            Text(
                text = "登录即表示同意网易云音乐用户协议",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun BackgroundDecoration() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_offset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-80 + offset * 20).dp, y = (-50 + offset * 30).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeteaseRed.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.BottomEnd)
                .offset(x = (40 - offset * 15).dp, y = (40 - offset * 20).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeteaseRedDark.copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
    }
}

@Composable
private fun LogoSection() {
    // 音符动画
    val infiniteTransition = rememberInfiniteTransition(label = "logo_anim")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        // 外圆发光效果
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeteaseRed.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )
        // 图标背景
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(NeteaseRed, NeteaseRedDark)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    Text(
        text = "My Music",
        style = MaterialTheme.typography.displayLarge.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        ),
        color = TextPrimary
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "聆听世界，感受音乐",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary
    )
}

@Composable
private fun LoginTabRow(
    activeTab: LoginTab,
    onTabChange: (LoginTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        LoginTabItem(
            text = "验证码",
            isActive = activeTab == LoginTab.CAPTCHA,
            onClick = { onTabChange(LoginTab.CAPTCHA) }
        )
        Spacer(modifier = Modifier.width(24.dp))
        LoginTabItem(
            text = "扫码登录",
            isActive = activeTab == LoginTab.QR,
            onClick = { onTabChange(LoginTab.QR) }
        )
    }
}

@Composable
private fun LoginTabItem(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) NeteaseRed else Color.Transparent,
        animationSpec = tween(300),
        label = "tab_color"
    )
    val textColor by animateColorAsState(
        targetValue = if (isActive) Color.White else TextSecondary,
        animationSpec = tween(300),
        label = "tab_text_color"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = textColor
        )
    }
}

@Composable
private fun CaptchaLoginForm(
    phone: String,
    captcha: String,
    isLoading: Boolean,
    isSendingCode: Boolean,
    codeSent: Boolean,
    onPhoneChange: (String) -> Unit,
    onCaptchaChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onLogin: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    // 倒计时
    var countdown by remember { mutableStateOf(0) }
    LaunchedEffect(codeSent) {
        if (codeSent) {
            countdown = 60
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 手机号输入框
        StyledTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = "手机号",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Next
        )

        // 验证码输入框 + 发送按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StyledTextField(
                    value = captcha,
                    onValueChange = onCaptchaChange,
                    label = "验证码",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onLogin()
                        }
                    )
                )
            }
            Button(
                onClick = onSendCode,
                enabled = !isSendingCode && countdown == 0,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeteaseRed,
                    disabledContainerColor = DarkElevated
                )
            ) {
                if (isSendingCode) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (countdown > 0) "${countdown}s" else "发送",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 登录按鈕
        Button(
            onClick = {
                focusManager.clearFocus()
                onLogin()
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (!isLoading)
                            Brush.horizontalGradient(colors = listOf(NeteaseRed, NeteaseRedDark))
                        else
                            Brush.horizontalGradient(colors = listOf(DarkElevated, DarkElevated)),
                        shape = RoundedCornerShape(50)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = TextSecondary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "登 录",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextTertiary) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = label,
                tint = if (value.isNotEmpty()) NeteaseRed else TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = keyboardActions,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeteaseRed,
            unfocusedBorderColor = DarkElevated,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = NeteaseRed,
            focusedContainerColor = DarkCard,
            unfocusedContainerColor = DarkCard
        )
    )
}

@Composable
private fun QrLoginForm(
    isLoading: Boolean,
    qrData: com.example.mymusic.data.model.QrCreateData?,
    statusMessage: String
) {
    // 在后台线程生成二维码 Bitmap，避免主线程卡帧
    var qrBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val qrurl = qrData?.qrurl
    LaunchedEffect(qrurl) {
        qrBitmap = null
        if (!qrurl.isNullOrEmpty()) {
            qrBitmap = withContext(Dispatchers.Default) {
                generateQrBitmap(qrurl, 512)?.asImageBitmap()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 二维码展示区域
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, DarkElevated, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading || qrData == null || (qrBitmap == null && !qrurl.isNullOrEmpty()) -> {
                    // 正在加载或正在生戛二维码
                    CircularProgressIndicator(
                        color = NeteaseRed,
                        strokeWidth = 3.dp
                    )
                }
                qrBitmap != null -> {
                    Image(
                        bitmap = qrBitmap!!,
                        contentDescription = "登录二维码",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
                else -> {
                    Text("二维码生成失败", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        // 状态文字
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = NeteaseRed,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        Text(
            text = "打开网易云音乐 App → 我的 → 扫一扫",
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary,
            textAlign = TextAlign.Center
        )
    }
}

/** 使用 ZXing 本地生成二维码 Bitmap，白底黑码 */
private fun generateQrBitmap(content: String, size: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}


@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = NeteaseRedDark.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, NeteaseRed.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = NeteaseRedLight,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
