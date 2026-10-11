package com.example.bna

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 退出登录状态机回归测试（纯逻辑复现，不需要 Android 运行时）。
 *
 * 复现并锁定本轮修复的 bug：「登录页闪一下就没了」。
 *
 * 真实链路中的角色：
 *  - cookie        = ApiClient.getCookie() 里是否含 MUSIC_U（唯一真相）
 *  - loginIsLoggedIn = LoginViewModel.uiState.isLoggedIn（Activity 作用域的缓存，可能陈旧）
 *  - LoginScreen 挂载时的判定 = uiState.isLoggedIn && hasActiveSession()
 *
 * 修复前的判定只有 loginIsLoggedIn，于是陈旧的 true 会让登录页首帧就跳走。
 */
class LogoutFlowTest {

    /** 模仿 LoginViewModel：保留一份可能陈旧的 isLoggedIn 缓存 + 以 cookie 为准的查询。 */
    private class FakeLoginState {
        var isLoggedIn: Boolean = false
        var cookie: String = ""

        fun hasActiveSession(): Boolean = cookie.contains("MUSIC_U", ignoreCase = true)

        fun onLoggedOut() {
            isLoggedIn = false
        }

        /** LoginScreen 首帧的复位逻辑（新增）。 */
        fun resetOnMountIfNoSession() {
            if (!hasActiveSession()) onLoggedOut()
        }

        /** LoginScreen 的导航判定（已加 Cookie 兜底）。 */
        fun shouldNavigateToMain(): Boolean = isLoggedIn && hasActiveSession()
    }

    @Test
    fun `修复前 - 陈旧缓存会让登录页误判为已登录`() {
        val s = FakeLoginState()
        // 启动时检测到 MUSIC_U，LoginViewModel 缓存 isLoggedIn = true
        s.cookie = "MUSIC_U=abc; os=iPad OS"
        s.isLoggedIn = true

        // 用户退出：SearchViewModel 只清了 Cookie，没有重置 LoginViewModel
        s.cookie = ""

        // 旧判定逻辑（只看缓存）→ 仍认为已登录 → 登录页立刻又跳回主界面
        assertTrue("旧逻辑会误判（正是 bug 成因）", s.isLoggedIn)
    }

    @Test
    fun `修复后 - 退出登录清 Cookie 后不应跳回主界面`() {
        val s = FakeLoginState()
        s.cookie = "MUSIC_U=abc; os=iPad OS"
        s.isLoggedIn = true

        // 退出：Cookie 被清 + MainScreen.handleLogout 调用了 onLoggedOut()
        s.cookie = ""
        s.onLoggedOut()

        assertFalse("修复后不应跳转", s.shouldNavigateToMain())
        assertFalse("未登录态", s.isLoggedIn)
    }

    @Test
    fun `修复后 - 即使忘了重置缓存 挂载复位也能兜住`() {
        val s = FakeLoginState()
        s.cookie = "MUSIC_U=abc"
        s.isLoggedIn = true

        // 只清 Cookie，没调 onLoggedOut（模拟遗漏路径）
        s.cookie = ""

        // LoginScreen 挂载时的复位必须生效
        s.resetOnMountIfNoSession()

        assertFalse("挂载复位后不应跳转", s.shouldNavigateToMain())
    }

    @Test
    fun `修复后 - 真正登录成功仍然能进入主界面`() {
        val s = FakeLoginState()
        s.isLoggedIn = false
        s.cookie = ""

        // 登录页挂载复位（此时本来就没登录，无副作用）
        s.resetOnMountIfNoSession()
        assertFalse(s.shouldNavigateToMain())

        // 用户完成登录：写入 MUSIC_U + isLoggedIn = true
        s.cookie = "MUSIC_U=xyz; __csrf=1"
        s.isLoggedIn = true

        assertTrue("登录成功后应放行", s.shouldNavigateToMain())
    }

    @Test
    fun `修复后 - 冷启动已登录不会被自己的复位逻辑踢出去`() {
        val s = FakeLoginState()
        // 冷启动：Cookie 有效，但 LoginScreen 尚未参与（走 Splash → main）
        s.cookie = "MUSIC_U=abc"
        s.isLoggedIn = true

        // 即便登录页的复位逻辑被调用，也因为有有效 Cookie 而不复位
        s.resetOnMountIfNoSession()

        assertTrue("有有效会话时不应被复位", s.isLoggedIn)
        assertTrue(s.shouldNavigateToMain())
    }
}
