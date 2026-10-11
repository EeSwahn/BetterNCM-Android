package com.example.bna

import com.example.bna.data.cache.MusicCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 缓存 key 与 TTL 判定的单元测试。
 *
 * 说明：内存/磁盘层依赖 Android `Context`（`filesDir`），属仪器测试范畴；
 * 这里覆盖**纯逻辑**部分 —— key 生成规则与新鲜度判定，这两处出错会导致
 * 「串号看到别人歌单」或「缓存永远不过期」这类难以察觉的问题。
 */
class MusicCacheLogicTest {

    @Test
    fun `歌单歌曲 key 按歌单 id 区分`() {
        assertEquals("playlist_songs_123", MusicCache.playlistSongsKey(123))
        assertEquals("playlist_songs_456", MusicCache.playlistSongsKey(456))
        assertTrue(
            "不同歌单的 key 必须不同，否则会串数据",
            MusicCache.playlistSongsKey(1) != MusicCache.playlistSongsKey(2)
        )
    }

    @Test
    fun `我的歌单 key 按 uid 区分（防切号串数据）`() {
        assertEquals("my_playlists_1567688233", MusicCache.myPlaylistsKey(1567688233))
        assertTrue(
            "不同账号的 key 必须不同",
            MusicCache.myPlaylistsKey(1) != MusicCache.myPlaylistsKey(2)
        )
    }

    @Test
    fun `刚写入的时间戳属于新鲜`() {
        assertTrue(MusicCache.isFresh(System.currentTimeMillis()))
    }

    @Test
    fun `超过 TTL 的时间戳属于过期`() {
        val justOver = System.currentTimeMillis() - MusicCache.DEFAULT_TTL_MILLIS - 1_000
        assertFalse(MusicCache.isFresh(justOver))
    }

    @Test
    fun `默认 TTL 为 30 分钟`() {
        assertEquals(30 * 60 * 1000L, MusicCache.DEFAULT_TTL_MILLIS)
        // 边界：刚好 TTL 内的应算新鲜
        assertTrue(MusicCache.isFresh(System.currentTimeMillis() - MusicCache.DEFAULT_TTL_MILLIS + 5_000))
        // 边界：刚好超过的算过期
        assertFalse(MusicCache.isFresh(System.currentTimeMillis() - MusicCache.DEFAULT_TTL_MILLIS - 5_000))
    }

    @Test
    fun `可自定义 TTL`() {
        val tenSecAgo = System.currentTimeMillis() - 10_000
        assertTrue("1 分钟 TTL 下 10 秒前仍新鲜", MusicCache.isFresh(tenSecAgo, 60_000))
        assertFalse("5 秒 TTL 下 10 秒前已过期", MusicCache.isFresh(tenSecAgo, 5_000))
    }
}
