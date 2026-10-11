package com.example.bna

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import com.example.bna.ui.animation.SaltMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁定 [SaltMotion] 的数值等于椒盐音乐 v11.0.4 的逆向结果。
 *
 * 这些数字来自反编译源码 `androidx.core.y51`（= Material3 `StandardMotionSchemeImpl`）
 * L11-26，经 `androidx.core.a61` 枚举 → `androidx.core.u44.m6380(...)` 映射。
 * 若有人手滑改了 [SaltMotion] 的值，本测试会立刻失败 —— 这是刻意的回归保护。
 */
class SaltMotionSpecTest {

    /** 把 FiniteAnimationSpec 断言成 SpringSpec 并回传。 */
    private fun asSpring(spec: FiniteAnimationSpec<Float>, name: String): SpringSpec<Float> {
        assertNotNull("$name 不应为 null", spec)
        assertTrue("$name 应为 SpringSpec（salt 全部用 spring），实际 ${spec::class}", spec is SpringSpec<*>)
        return spec as SpringSpec<Float>
    }

    // ── "空间"曲线：dampingRatio = 0.9f（有轻微回弹）──

    @Test
    fun `DefaultSpatial 等于 salt 的 0_9 _700`() {
        val s = asSpring(SaltMotion.DefaultSpatial, "DefaultSpatial")
        assertEquals("y51.f15789 = t54.m6031(0.9f, 700.0f, null)", 0.9f, s.dampingRatio!!, 0f)
        assertEquals("y51.f15789 = t54.m6031(0.9f, 700.0f, null)", 700f, s.stiffness!!, 0f)
    }

    @Test
    fun `FastSpatial 等于 salt 的 0_9 _1400`() {
        val s = asSpring(SaltMotion.FastSpatial, "FastSpatial")
        assertEquals("y51.f15790 = t54.m6031(0.9f, 1400.0f, null)", 0.9f, s.dampingRatio!!, 0f)
        assertEquals("y51.f15790 = t54.m6031(0.9f, 1400.0f, null)", 1400f, s.stiffness!!, 0f)
    }

    @Test
    fun `SlowSpatial 等于 salt 的 0_9 _300`() {
        val s = asSpring(SaltMotion.SlowSpatial, "SlowSpatial")
        assertEquals("y51.f15791 = t54.m6031(0.9f, 300.0f, null)", 0.9f, s.dampingRatio!!, 0f)
        assertEquals("y51.f15791 = t54.m6031(0.9f, 300.0f, null)", 300f, s.stiffness!!, 0f)
    }

    // ── "效果"曲线：dampingRatio = 1.0f（临界阻尼，不过冲）──

    @Test
    fun `DefaultEffects 等于 salt 的 1_0 _1600 且被页面切换使用`() {
        val s = asSpring(SaltMotion.DefaultEffects, "DefaultEffects")
        assertEquals("y51.f15792 = t54.m6031(1.0f, 1600.0f, null)", 1.0f, s.dampingRatio!!, 0f)
        assertEquals("y51.f15792 = t54.m6031(1.0f, 1600.0f, null)", 1600f, s.stiffness!!, 0f)
        // 页面切换必须就是 DefaultEffects（salt g52.java:28）
        assertEquals(
            "pageSwitch 必须等于 DefaultEffects（salt g52.java:28 的 Crossfade 参数）",
            s, SaltMotion.pageSwitch
        )
    }

    @Test
    fun `FastEffects 等于 salt 的 1_0 _3800`() {
        val s = asSpring(SaltMotion.FastEffects, "FastEffects")
        assertEquals("y51.f15793 = t54.m6031(1.0f, 3800.0f, null)", 1.0f, s.dampingRatio!!, 0f)
        assertEquals("y51.f15793 = t54.m6031(1.0f, 3800.0f, null)", 3800f, s.stiffness!!, 0f)
    }

    @Test
    fun `SlowEffects 等于 salt 的 1_0 _800`() {
        val s = asSpring(SaltMotion.SlowEffects, "SlowEffects")
        assertEquals("y51.f15794 = t54.m6031(1.0f, 800.0f, null)", 1.0f, s.dampingRatio!!, 0f)
        assertEquals("y51.f15794 = t54.m6031(1.0f, 800.0f, null)", 800f, s.stiffness!!, 0f)
    }

    // ── 结构约束：salt 只有两种阻尼比 ──

    @Test
    fun `空间类阻尼比全为 0_9 且效果类全为 1_0`() {
        val spatial = listOf(
            "DefaultSpatial" to SaltMotion.DefaultSpatial,
            "FastSpatial" to SaltMotion.FastSpatial,
            "SlowSpatial" to SaltMotion.SlowSpatial
        )
        val effects = listOf(
            "DefaultEffects" to SaltMotion.DefaultEffects,
            "FastEffects" to SaltMotion.FastEffects,
            "SlowEffects" to SaltMotion.SlowEffects
        )
        spatial.forEach { (n, spec) ->
            assertEquals("$n 属于空间类，阻尼比必须是 0.9f", 0.9f, asSpring(spec, n).dampingRatio!!, 0f)
        }
        effects.forEach { (n, spec) ->
            assertEquals("$n 属于效果类，阻尼比必须是 1.0f", 1.0f, asSpring(spec, n).dampingRatio!!, 0f)
        }
    }

    @Test
    fun `刚度排序符合 快 大于 默认 大于 慢`() {
        assertTrue(
            "FastSpatial 应比 DefaultSpatial 更快",
            (SaltMotion.FastSpatial as SpringSpec<Float>).stiffness!! >
                (SaltMotion.DefaultSpatial as SpringSpec<Float>).stiffness!!
        )
        assertTrue(
            "DefaultSpatial 应比 SlowSpatial 更快",
            (SaltMotion.DefaultSpatial as SpringSpec<Float>).stiffness!! >
                (SaltMotion.SlowSpatial as SpringSpec<Float>).stiffness!!
        )
        assertTrue(
            "FastEffects 应比 DefaultEffects 更快",
            (SaltMotion.FastEffects as SpringSpec<Float>).stiffness!! >
                (SaltMotion.DefaultEffects as SpringSpec<Float>).stiffness!!
        )
        assertTrue(
            "DefaultEffects 应比 SlowEffects 更快",
            (SaltMotion.DefaultEffects as SpringSpec<Float>).stiffness!! >
                (SaltMotion.SlowEffects as SpringSpec<Float>).stiffness!!
        )
    }
}
