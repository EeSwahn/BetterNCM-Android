package com.example.bna.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.bna.data.model.Song
import com.example.bna.data.repository.MusicRepository
import com.example.bna.data.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

enum class PlaybackMode {
    LIST_LOOP,
    SINGLE_LOOP,
    SHUFFLE
}

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val playUrl: String? = null,
    val playlist: List<Song> = emptyList(),
    val currentIndex: Int = -1,
    val playbackMode: PlaybackMode = PlaybackMode.LIST_LOOP
)

object MusicPlayer {
    private data class QueueItem(
        val song: Song,
        val playUrl: String? = null
    )

    private var _exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private val repository = MusicRepository()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var queue = mutableListOf<QueueItem>()
    private var currentQueueIndex = -1
    private var pendingRequestId = 0L
    private var playbackMode = PlaybackMode.LIST_LOOP
    private val shuffleHistory = ArrayDeque<Int>()

    val suiXinChangProcessor = SuiXinChangAudioProcessor()

    var vocalVolume: Float
        get() = suiXinChangProcessor.vocalVolume
        set(value) {
            suiXinChangProcessor.vocalVolume = value
        }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun initPlayer(context: Context) {
        if (_exoPlayer != null) return

        val renderersFactory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf(suiXinChangProcessor))
                    .build()
            }
        }

        _exoPlayer = ExoPlayer.Builder(context.applicationContext)
            .setRenderersFactory(renderersFactory)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    syncPlayerState(isPlaying = isPlaying)
                    if (isPlaying) {
                        progressJob?.cancel()
                        progressJob = scope.launch {
                            while (true) {
                                syncPlayerState(currentPosition = currentPosition)
                                delay(32L) // Approx ~30+ fps update rate
                            }
                        }
                    } else {
                        progressJob?.cancel()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            syncPlayerState(isLoading = true)
                        }
                        Player.STATE_READY -> {
                            syncPlayerState(
                                isLoading = false,
                                duration = duration,
                                currentPosition = currentPosition,
                                error = null
                            )
                        }
                        Player.STATE_ENDED -> {
                            if (playbackMode == PlaybackMode.SINGLE_LOOP) {
                                seekTo(0)
                                play()
                            } else {
                                val nextIndex = resolveNextIndex(isManual = false)
                                if (nextIndex != null) {
                                    playAtIndex(nextIndex, recordShuffleHistory = playbackMode == PlaybackMode.SHUFFLE)
                                } else {
                                    syncPlayerState(
                                        isPlaying = false,
                                        isLoading = false,
                                        currentPosition = duration.coerceAtLeast(0L)
                                    )
                                }
                            }
                        }
                        Player.STATE_IDLE -> {
                            syncPlayerState(isLoading = false)
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    syncPlayerState(
                        isLoading = false,
                        isPlaying = false,
                        error = "播放错误: ${error.message}"
                    )
                }
            })
        }
    }

    fun cyclePlaybackMode() {
        playbackMode = when (playbackMode) {
            PlaybackMode.LIST_LOOP -> PlaybackMode.SINGLE_LOOP
            PlaybackMode.SINGLE_LOOP -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.LIST_LOOP
        }
        if (playbackMode != PlaybackMode.SHUFFLE) {
            shuffleHistory.clear()
        }
        syncPlayerState()
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        playbackMode = mode
        if (mode != PlaybackMode.SHUFFLE) {
            shuffleHistory.clear()
        }
        syncPlayerState()
    }

    fun playSong(song: Song, url: String) {
        val existingIndex = queue.indexOfFirst { it.song.id == song.id }
        if (existingIndex >= 0) {
            queue[existingIndex] = queue[existingIndex].copy(song = song, playUrl = url)
            currentQueueIndex = existingIndex
        } else {
            queue = mutableListOf(QueueItem(song = song, playUrl = url))
            currentQueueIndex = 0
        }
        shuffleHistory.clear()
        playAtIndex(currentQueueIndex, providedUrl = url)
    }

    fun playSongs(songs: List<Song>, startIndex: Int, startUrl: String? = null) {
        if (songs.isEmpty()) return
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        queue = songs.mapIndexed { index, song ->
            QueueItem(song = song, playUrl = if (index == safeIndex) startUrl else null)
        }.toMutableList()
        currentQueueIndex = safeIndex
        shuffleHistory.clear()
        playAtIndex(safeIndex, providedUrl = startUrl)
    }

    fun enqueueNext(song: Song) {
        if (queue.isEmpty() || currentQueueIndex !in queue.indices) {
            queue = mutableListOf(QueueItem(song))
            currentQueueIndex = 0
            shuffleHistory.clear()
            playAtIndex(0)
            return
        }

        if (queue[currentQueueIndex].song.id == song.id) {
            return
        }

        val existingIndex = queue.indexOfFirst { it.song.id == song.id }
        val queueItem = if (existingIndex >= 0) {
            val removed = queue.removeAt(existingIndex)
            if (existingIndex < currentQueueIndex) {
                currentQueueIndex -= 1
            }
            removed.copy(song = song)
        } else {
            QueueItem(song = song)
        }

        val insertIndex = (currentQueueIndex + 1).coerceAtMost(queue.size)
        queue.add(insertIndex, queueItem)
        syncPlayerState()
    }

    fun playSongAt(index: Int) {
        if (index !in queue.indices) return
        playAtIndex(index, recordShuffleHistory = playbackMode == PlaybackMode.SHUFFLE)
    }

    fun playPrevious() {
        val player = _exoPlayer ?: return
        if (player.currentPosition > 3_000L) {
            player.seekTo(0)
            return
        }

        if (playbackMode == PlaybackMode.SHUFFLE) {
            val previousIndex = consumeShufflePreviousIndex()
            if (previousIndex != null) {
                playAtIndex(previousIndex, recordShuffleHistory = false)
            } else {
                player.seekTo(0)
            }
            return
        }

        val previousIndex = resolvePreviousIndex() ?: run {
            player.seekTo(0)
            return
        }
        playAtIndex(previousIndex)
    }

    fun playNext() {
        val nextIndex = resolveNextIndex(isManual = true) ?: return
        playAtIndex(nextIndex, recordShuffleHistory = playbackMode == PlaybackMode.SHUFFLE)
    }

    fun togglePlayPause() {
        val player = _exoPlayer ?: return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        _exoPlayer?.seekTo(positionMs)
    }

    fun getCurrentPosition(): Long = _exoPlayer?.currentPosition ?: 0L

    fun getDuration(): Long = _exoPlayer?.duration ?: 0L

    fun stop() {
        _exoPlayer?.stop()
        progressJob?.cancel()
        syncPlayerState(
            isPlaying = false,
            isLoading = false,
            currentPosition = 0L
        )
    }

    fun clearQueue() {
        pendingRequestId += 1
        progressJob?.cancel()
        _exoPlayer?.stop()
        queue.clear()
        currentQueueIndex = -1
        shuffleHistory.clear()
        _playerState.value = PlayerState(playbackMode = playbackMode)
    }

    fun release() {
        progressJob?.cancel()
        _exoPlayer?.release()
        _exoPlayer = null
    }

    fun clearError() {
        syncPlayerState(error = null)
    }

    private fun resolveNextIndex(isManual: Boolean): Int? {
        if (queue.isEmpty() || currentQueueIndex !in queue.indices) return null

        return when (playbackMode) {
            PlaybackMode.LIST_LOOP -> {
                if (queue.size == 1) currentQueueIndex else (currentQueueIndex + 1) % queue.size
            }
            PlaybackMode.SINGLE_LOOP -> {
                if (!isManual) currentQueueIndex
                else if (queue.size == 1) currentQueueIndex
                else (currentQueueIndex + 1) % queue.size
            }
            PlaybackMode.SHUFFLE -> pickRandomIndexExcluding(currentQueueIndex)
        }
    }

    private fun resolvePreviousIndex(): Int? {
        if (queue.isEmpty() || currentQueueIndex !in queue.indices) return null
        return if (queue.size == 1) {
            currentQueueIndex
        } else {
            if (currentQueueIndex == 0) queue.lastIndex else currentQueueIndex - 1
        }
    }

    private fun consumeShufflePreviousIndex(): Int? {
        while (shuffleHistory.isNotEmpty()) {
            val previousIndex = shuffleHistory.removeLast()
            if (previousIndex in queue.indices && previousIndex != currentQueueIndex) {
                return previousIndex
            }
        }
        return null
    }

    private fun pickRandomIndexExcluding(indexToExclude: Int): Int? {
        if (queue.isEmpty()) return null
        if (queue.size == 1) return currentQueueIndex.takeIf { it >= 0 } ?: 0

        val candidates = queue.indices.filter { it != indexToExclude }
        return candidates.randomOrNull(Random.Default)
    }

    private fun rememberCurrentForShuffle(nextIndex: Int) {
        if (playbackMode != PlaybackMode.SHUFFLE) return
        if (currentQueueIndex !in queue.indices) return
        if (currentQueueIndex == nextIndex) return
        if (shuffleHistory.lastOrNull() == currentQueueIndex) return
        shuffleHistory.addLast(currentQueueIndex)
        while (shuffleHistory.size > queue.size * 2) {
            shuffleHistory.removeFirst()
        }
    }

    private fun playAtIndex(
        index: Int,
        providedUrl: String? = null,
        recordShuffleHistory: Boolean = false
    ) {
        val player = _exoPlayer ?: return
        if (index !in queue.indices) return

        if (recordShuffleHistory) {
            rememberCurrentForShuffle(index)
        }

        pendingRequestId += 1
        currentQueueIndex = index
        val currentItem = queue[index]
        val resolvedUrl = providedUrl ?: currentItem.playUrl
        if (resolvedUrl != null && currentItem.playUrl != resolvedUrl) {
            queue[index] = currentItem.copy(playUrl = resolvedUrl)
        }

        syncPlayerState(
            isLoading = true,
            isPlaying = false,
            currentPosition = 0L,
            duration = 0L,
            error = null,
            playUrl = resolvedUrl
        )

        if (resolvedUrl != null) {
            prepareAndPlay(player, resolvedUrl)
            return
        }

        val requestId = pendingRequestId
        val songId = currentItem.song.id
        scope.launch {
            when (val result = repository.getSongUrl(songId)) {
                is Result.Success -> {
                    if (requestId != pendingRequestId) return@launch
                    val latestItem = queue.getOrNull(index) ?: return@launch
                    if (latestItem.song.id != songId || currentQueueIndex != index) return@launch
                    queue[index] = latestItem.copy(playUrl = result.data)
                    syncPlayerState(
                        isLoading = true,
                        isPlaying = false,
                        currentPosition = 0L,
                        duration = 0L,
                        error = null,
                        playUrl = result.data
                    )
                    prepareAndPlay(player, result.data)
                }
                is Result.Error -> {
                    if (requestId != pendingRequestId) return@launch
                    syncPlayerState(
                        isLoading = false,
                        isPlaying = false,
                        error = result.message
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun prepareAndPlay(player: ExoPlayer, url: String) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    private fun syncPlayerState(
        isPlaying: Boolean = _playerState.value.isPlaying,
        currentPosition: Long = _playerState.value.currentPosition,
        duration: Long = _playerState.value.duration,
        isLoading: Boolean = _playerState.value.isLoading,
        error: String? = _playerState.value.error,
        playUrl: String? = queue.getOrNull(currentQueueIndex)?.playUrl
    ) {
        val currentItem = queue.getOrNull(currentQueueIndex)
        _playerState.value = _playerState.value.copy(
            currentSong = currentItem?.song,
            isPlaying = isPlaying,
            currentPosition = currentPosition,
            duration = duration,
            isLoading = isLoading,
            error = error,
            playUrl = playUrl,
            playlist = queue.map { it.song },
            currentIndex = if (currentItem != null) currentQueueIndex else -1,
            playbackMode = playbackMode
        )
    }
}
