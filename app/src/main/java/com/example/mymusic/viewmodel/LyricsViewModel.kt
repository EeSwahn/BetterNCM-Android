package com.example.mymusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.model.LyricLine
import com.example.mymusic.data.repository.MusicRepository
import com.example.mymusic.data.repository.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LyricsUiState(
    val lyrics: List<LyricLine> = emptyList(),
    val currentLineIndex: Int = -1,
    val isLoading: Boolean = false,
    val hasNoLyric: Boolean = false,
    val suiXinChangLoading: Boolean = false,
    val suiXinChangActive: Boolean = false,
    val suiXinChangVolume: Float = 1.0f,
    val suiXinChangSliderVisible: Boolean = false
)

class LyricsViewModel : ViewModel() {

    private val repository = MusicRepository()

    private val _state = MutableStateFlow(LyricsUiState())
    val state: StateFlow<LyricsUiState> = _state.asStateFlow()

    private var loadedSongId: Long = -1L

    fun loadLyrics(songId: Long) {
        if (loadedSongId == songId) return
        loadedSongId = songId
        _state.value = LyricsUiState(isLoading = true)

        // songId = -1L 代表本地测试歌曲，使用内置歌词
        if (songId == -1L) {
            val lines = parseLrc(BUILTIN_LRC_SAY_LOVE_YOU)
            _state.value = LyricsUiState(lyrics = lines)
            return
        }

        viewModelScope.launch {
            when (val result = repository.getLyric(songId)) {
                is Result.Success -> {
                    val lrcText   = result.data.lrc?.lyric    ?: ""
                    val transText = result.data.tlyric?.lyric ?: ""
                    val lines = parseLrc(lrcText, transText)
                    if (lines.isEmpty()) {
                        _state.value = LyricsUiState(hasNoLyric = true)
                    } else {
                        _state.value = LyricsUiState(lyrics = lines)
                    }
                }
                else -> {
                    _state.value = LyricsUiState(hasNoLyric = true)
                }
            }
        }
    }

    /** 外部每 100ms 调用一次，传入当前播放位置，自动计算高亮行 */
    fun updateCurrentLine(positionMs: Long) {
        val lyrics = _state.value.lyrics
        if (lyrics.isEmpty()) return
        val idx = lyrics.indexOfLast { it.timeMs <= positionMs }
        if (idx != _state.value.currentLineIndex) {
            _state.value = _state.value.copy(currentLineIndex = idx)
        }
    }

    fun reset() {
        loadedSongId = -1L
        _state.value = LyricsUiState()
    }

    fun toggleSuiXinChang(song: com.example.mymusic.data.model.Song) {
        val currentState = _state.value
        if (currentState.suiXinChangActive) {
            return
        }

        if (currentState.suiXinChangLoading) return

        _state.value = currentState.copy(suiXinChangLoading = true)
        viewModelScope.launch {
            val accompanyUrl = com.example.mymusic.data.network.SuiXinChangApi.getAccompanyUrl(song.id)
            if (accompanyUrl != null) {
                // Return accompany url and play it
                com.example.mymusic.player.MusicPlayer.playSong(song, accompanyUrl)
                _state.value = _state.value.copy(
                    suiXinChangActive = true,
                    suiXinChangLoading = false
                )
            } else {
                _state.value = _state.value.copy(
                    suiXinChangLoading = false
                )
            }
        }
    }

    fun setSuiXinChangVolume(volume: Float) {
        _state.value = _state.value.copy(suiXinChangVolume = volume)
        // Apply cubic curve for better sensitivity from 0.7~1.0
        val curvedVolume = volume * volume * volume
        com.example.mymusic.player.MusicPlayer.vocalVolume = curvedVolume
    }

    fun showSuiXinChangSlider() {
        _state.value = _state.value.copy(suiXinChangSliderVisible = true)
    }

    fun hideSuiXinChangSlider() {
        _state.value = _state.value.copy(suiXinChangSliderVisible = false)
    }
}

// ─── LRC 解析 ──────────────────────────────────────────────────────────────

/**
 * 解析 LRC 格式歌词，支持单行多时间戳、翻译合并。
 * e.g.  [00:12.34]歌词文本
 *        [01:23.456]另一句
 */
private val TIME_PATTERN = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\]""")

private fun parseToMap(text: String): Map<Long, String> {
    val map = mutableMapOf<Long, String>()
    for (line in text.lines()) {
        val trimmed = line.trim()
        val timestamps = TIME_PATTERN.findAll(trimmed).map { m ->
            val min = m.groupValues[1].toLong()
            val sec = m.groupValues[2].toLong()
            val msRaw = m.groupValues[3]
            val ms = if (msRaw.length == 3) msRaw.toLong() else msRaw.toLong() * 10L
            min * 60_000L + sec * 1_000L + ms
        }.toList()
        if (timestamps.isEmpty()) continue
        val content = trimmed.replace(TIME_PATTERN, "").trim()
        if (content.isBlank()) continue
        timestamps.forEach { map[it] = content }
    }
    return map
}

fun parseLrc(lrcText: String, transText: String = ""): List<LyricLine> {
    val main  = parseToMap(lrcText)
    val trans = parseToMap(transText)
    return main.entries
        .map { (time, text) -> LyricLine(time, text, trans[time]) }
        .sortedBy { it.timeMs }
}

// ─── 内置测试歌词：蔡依林《说爱你》 ─────────────────────────────────────────
private const val BUILTIN_LRC_SAY_LOVE_YOU = """
[00:20.88]我的世界变得奇妙
[00:23.43]更难以言喻
[00:25.62]还以为是从天而降的梦境
[00:30.34]直到确定手的温度
[00:32.84]来自你心里
[00:34.97]这一刻我终于勇敢说爱你
[00:40.22]一开始我只顾着看你
[00:42.26]装做不经意心却飘过去
[00:44.59]还窃喜你没发现我躲在角落
[00:49.16]忙着快乐忙着感动
[00:51.40]从彼此陌生到熟
[00:52.67]会是我们从没想过
[00:54.81]真爱到现在不敢期待
[00:59.43]要证明自己曾被你
[01:02.39]想起 Really
[01:04.42]我胡思乱想
[01:06.10]就从今天起 I wish
[01:09.10]像一个陷阱却从未犹豫相信
[01:13.73]你真的愿意就请给我惊喜
[01:17.39]关于爱情过去
[01:19.22]没有异想的结局
[01:21.97]那天起却颠覆了自己逻辑
[01:26.70]我的怀疑所有答案
[01:29.24]因你而明白
[01:31.64]转啊转就真的遇见 Mr.Right
[01:55.58]一开始我只顾着看你
[01:57.55]装做不经意心却飘过去
[01:59.89]还窃喜你没发现我躲在角落
[02:04.47]忙着快乐忙着感动
[02:06.66]从彼此陌生到熟
[02:07.83]会是我们从没想过
[02:10.07]真爱到现在不敢期待
[02:14.95]要证明自己曾被你
[02:17.59]想起 Really
[02:19.72]我胡思乱想
[02:21.35]就从今天起 I wish
[02:24.35]像一个陷阱却从未犹豫相信
[02:29.09]你真的愿意就请给我惊喜
[02:32.65]关于爱情过去
[02:34.59]没有异想的结局
[02:37.58]那天起却颠覆了自己逻辑
[02:42.11]我的怀疑所有答案
[02:44.55]因你而明白
[02:47.05]转啊转就真的遇见 Mr.Right
[02:51.52]我的世界变得奇妙
[02:53.97]更难以言喻
[02:56.36]还以为是从天而降的梦境
[03:00.78]直到确定手的温度
[03:03.38]来自你心里
[03:05.77]这一刻也终于勇敢说爱你
"""

