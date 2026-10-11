package com.example.bna.data.cache

import android.content.Context
import com.example.bna.data.model.Playlist
import com.example.bna.data.model.Song
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 两级缓存（内存 LRU + 磁盘 JSON），用于歌单与歌单歌曲。
 *
 * 设计取向与主流做法一致：
 *  - **内存层**：`LinkedHashMap(accessOrder = true)` 实现 LRU，命中即秒开，限额 [MEMORY_MAX_ENTRIES]；
 *  - **磁盘层**：每个 key 一个 JSON 文件，落盘在 `filesDir/cache_music/`，
 *    冷启动仍可秒开；限额 [DISK_MAX_FILES]，超限按文件最后修改时间淘汰最旧；
 *  - **TTL**：超过 [ttlMillis] 视为过期。过期不强删，而是**先回显旧数据再后台刷新**
 *    （详见调用方），保证「秒开 + 最终一致」，不会出现空白转圈。
 *
 * 线程安全：读写都在调用方协程（IO 调度器）里串行处理；
 * 内存层用 [ConcurrentHashMap] + 同步块保护 LRU 顺序。
 */
object MusicCache {

    private const val DIR_NAME = "cache_music"
    private const val MEMORY_MAX_ENTRIES = 32
    private const val DISK_MAX_FILES = 200

    /** 歌单歌曲缓存有效期：30 分钟。歌单列表同样 30 分钟。 */
    const val DEFAULT_TTL_MILLIS = 30 * 60 * 1000L

    @PublishedApi internal val gson = Gson()

    private lateinit var cacheDir: File

    /** 内存 LRU：accessOrder=true 时 get 会把条目移到队尾（最近使用）。 */
    private val memory = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {}

    @PublishedApi
    internal data class Entry(val json: String, val savedAt: Long)

    fun init(context: Context) {
        synchronized(this) {
            if (::cacheDir.isInitialized) return
            cacheDir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        }
    }

    // ========== 泛型读写 ==========

    /**
     * 读缓存。返回 null 表示「没有可用数据」（未命中）。
     *
     * 注意：**即使已过期也会返回数据**，由调用方决定是否后台刷新。
     * 这样可以实现「先显示旧内容、再静默更新」，而不是转圈等待。
     */
    inline fun <reified T> read(key: String): CacheResult<T>? {
        val raw = readRaw(key) ?: return null
        return try {
            val type = object : TypeToken<T>() {}.type
            val value: T = gson.fromJson(raw.json, type) ?: return null
            CacheResult(value, raw.savedAt, isFresh(raw.savedAt))
        } catch (e: Exception) {
            // 结构变更 / 文件损坏：丢掉这条缓存，别让脏数据卡住界面
            remove(key)
            null
        }
    }

    /** 写缓存（内存 + 磁盘同时写）。 */
    inline fun <reified T> write(key: String, value: T) {
        try {
            val type = object : TypeToken<T>() {}.type
            putRaw(key, gson.toJson(value, type))
        } catch (e: Exception) {
            // 序列化失败不应影响主流程
            println("DEBUG MusicCache: write failed for $key: ${e.message}")
        }
    }

    data class CacheResult<T>(val value: T, val savedAt: Long, val isFresh: Boolean)

    fun isFresh(savedAt: Long, ttl: Long = DEFAULT_TTL_MILLIS): Boolean =
        System.currentTimeMillis() - savedAt < ttl

    // ========== 内部实现（非 inline，供 inline 函数调用） ==========

    @PublishedApi internal fun readRaw(key: String): Entry? {
        synchronized(this) {
            memory[key]?.let { return it }
        }
        if (!::cacheDir.isInitialized) return null
        val f = fileFor(key)
        if (!f.exists()) return null
        return try {
            val text = f.readText()
            val obj = gson.fromJson(text, DiskEntry::class.java)
            if (obj?.json.isNullOrEmpty()) return null
            val entry = Entry(obj.json, obj.savedAt)
            synchronized(this) { memory[key] = entry }
            entry
        } catch (e: Exception) {
            runCatching { f.delete() }
            null
        }
    }

    @PublishedApi internal fun putRaw(key: String, json: String) {
        val entry = Entry(json, System.currentTimeMillis())
        synchronized(this) {
            memory[key] = entry
            trimMemory()
        }
        if (!::cacheDir.isInitialized) return
        try {
            fileFor(key).writeText(gson.toJson(DiskEntry(json, entry.savedAt)))
            trimDisk()
        } catch (e: Exception) {
            println("DEBUG MusicCache: disk write failed: ${e.message}")
        }
    }

    /** 删除单个 key 的两级缓存。 */
    fun remove(key: String) {
        synchronized(this) { memory.remove(key) }
        if (::cacheDir.isInitialized) runCatching { fileFor(key).delete() }
    }

    /** 清空全部缓存（退出登录 / 切换账号时调用）。 */
    fun clearAll() {
        synchronized(this) { memory.clear() }
        if (!::cacheDir.isInitialized) return
        runCatching { cacheDir.listFiles()?.forEach { it.delete() } }
    }

    /** 移除某类前缀的全部缓存，如所有 `playlist_songs_*`。 */
    fun removeByPrefix(prefix: String) {
        synchronized(this) {
            memory.keys.filter { it.startsWith(prefix) }.forEach { memory.remove(it) }
        }
        if (!::cacheDir.isInitialized) return
        runCatching {
            cacheDir.listFiles()?.forEach { if (it.name.startsWith(safeName(prefix))) it.delete() }
        }
    }

    private fun trimMemory() {
        // synchronized 块内调用；LinkedHashMap 迭代顺序即最久未使用在前
        while (memory.size > MEMORY_MAX_ENTRIES) {
            val oldest = memory.keys.firstOrNull() ?: break
            memory.remove(oldest)
        }
    }

    private fun trimDisk() {
        val files = cacheDir.listFiles() ?: return
        if (files.size <= DISK_MAX_FILES) return
        files.sortedBy { it.lastModified() }
            .take(files.size - DISK_MAX_FILES)
            .forEach { runCatching { it.delete() } }
    }

    private data class DiskEntry(val json: String, val savedAt: Long)

    /** key → 文件名。key 里可能含 `/` 等字符，统一转义。 */
    private fun safeName(key: String): String =
        key.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private fun fileFor(key: String): File = File(cacheDir, safeName(key) + ".json")

    // ========== 业务 key ==========

    /** 登录用户歌单列表的缓存 key（按 uid 区分，避免切号串数据）。 */
    fun myPlaylistsKey(uid: Long): String = "my_playlists_$uid"

    /** 歌单歌曲的缓存 key。 */
    fun playlistSongsKey(playlistId: Long): String = "playlist_songs_$playlistId"

    /** 便捷：读歌单歌曲。 */
    fun readPlaylistSongs(playlistId: Long): CacheResult<List<Song>>? =
        read<List<Song>>(playlistSongsKey(playlistId))

    /** 便捷：写歌单歌曲。 */
    fun writePlaylistSongs(playlistId: Long, songs: List<Song>) =
        write(playlistSongsKey(playlistId), songs)

    /** 便捷：读我的歌单。 */
    fun readMyPlaylists(uid: Long): CacheResult<List<Playlist>>? =
        read<List<Playlist>>(myPlaylistsKey(uid))

    /** 便捷：写我的歌单。 */
    fun writeMyPlaylists(uid: Long, playlists: List<Playlist>) =
        write(myPlaylistsKey(uid), playlists)

    /** 退出登录时清掉与账号相关的缓存。 */
    fun clearUserData() {
        removeByPrefix("my_playlists_")
        removeByPrefix("playlist_songs_")
    }
}
