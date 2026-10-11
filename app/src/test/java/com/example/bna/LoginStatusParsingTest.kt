package com.example.bna

import com.example.bna.data.model.LoginStatusResponse
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 登录状态响应解析回归测试。
 *
 * 锁定「歌单区一直提示未获取到登录用户信息」的 bug：
 * 本项目**直连官方** `weapi/w/nuser/account/get`，该上游把 account / profile 放在**顶层**，
 * 没有 `data` 包裹层；而模型曾经只声明了 `data.profile`，于是 `data` 恒为 null。
 *
 * 下面用真实抓取到的官方响应形状（已脱敏）作为输入。
 */
class LoginStatusParsingTest {

    private val gson = Gson()

    /** 官方直连的真实形状（顶层 account/profile，无 data）—— 曾导致 bug 的那种。 */
    private val officialRaw = """
        {"code":200,
         "account":{"id":1567688233,"userName":"1_********038","type":1,"status":0,"vipType":11},
         "profile":{"userId":1567688233,"nickname":"EeSwahn","avatarUrl":"https://p4.music.126.net/x.jpg"}}
    """.trimIndent()

    /** api-enhanced 兼容形状（包了一层 data）—— 修复后也应能解析。 */
    private val enhancedWrapped = """
        {"code":200,
         "data":{"code":200,
                 "account":{"id":1567688233,"userName":"1_***","status":0},
                 "profile":{"userId":1567688233,"nickname":"EeSwahn"}}}
    """.trimIndent()

    @Test
    fun `官方顶层形状 - 能取到 uid 与昵称`() {
        val resp = gson.fromJson(officialRaw, LoginStatusResponse::class.java)!!

        assertEquals(1567688233L, resp.userId)
        assertEquals("EeSwahn", resp.effectiveProfile?.nickname)
        assertNotNull(resp.effectiveAccount)
    }

    @Test
    fun `官方顶层形状 - 映射前 data 为 null（正是旧 bug 的成因）`() {
        val resp = gson.fromJson(officialRaw, LoginStatusResponse::class.java)!!

        // 官方响应没有 data 字段 —— 所以「只读 data.profile」的旧逻辑必然拿到 null
        assertNull("官方响应不应有 data 包裹层", resp.data)
        // 而统一取值的 accessor 必须仍能拿到 uid
        assertEquals(1567688233L, resp.userId)
    }

    @Test
    fun `api-enhanced 包裹形状 - 同样能解析（向后兼容）`() {
        val resp = gson.fromJson(enhancedWrapped, LoginStatusResponse::class.java)!!

        assertEquals(1567688233L, resp.userId)
        assertEquals("EeSwahn", resp.effectiveProfile?.nickname)
    }

    @Test
    fun `未登录 - userId 为 null 且不抛异常`() {
        val resp = gson.fromJson("""{"code":200,"account":null,"profile":null}""",
            LoginStatusResponse::class.java)!!

        assertNull(resp.userId)
        assertNull(resp.effectiveProfile)
    }

    @Test
    fun `uid 为 0 或负数时视为无效`() {
        val resp = gson.fromJson("""{"code":200,"profile":{"userId":0,"nickname":"x"}}""",
            LoginStatusResponse::class.java)!!

        assertNull("userId=0 不应被当作有效登录用户", resp.userId)
    }
}
