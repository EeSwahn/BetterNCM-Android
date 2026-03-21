package com.example.bna

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bna.data.network.ApiClient
import com.example.bna.service.MusicService
import com.example.bna.ui.screen.LoginScreen
import com.example.bna.ui.screen.MainScreen
import com.example.bna.ui.screen.SplashScreen
import com.example.bna.ui.theme.BnaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 ApiClient（SharedPrefs），加载已保存的 Cookie 和 Base URL
        ApiClient.init(this)

        // 启动播放后台服务
        val serviceIntent = Intent(this, MusicService::class.java)
        startForegroundService(serviceIntent)

        enableEdgeToEdge()
        setContent {
            BnaTheme {
                AppNavigation(showSplashOnLaunch = savedInstanceState == null)
            }
        }
    }
}

@Composable
fun AppNavigation(showSplashOnLaunch: Boolean) {
    val navController = rememberNavController()
    val hasSavedSession = ApiClient.getCookie().contains("MUSIC_U", ignoreCase = true)
    val startDestination = when {
        showSplashOnLaunch -> "splash"
        hasSavedSession -> "main"
        else -> "login"
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("splash") {
            SplashScreen(
                onLoginValid = {
                    navController.navigate("main") {
                        popUpTo("splash") { inclusive = true }
                    }
                },
                onNeedLogin = {
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        // 登录成功后从返回栈中清除 login 屏，防止用户按返回键回到登录页
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                onLogout = {
                    navController.navigate("login") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }
    }
}
