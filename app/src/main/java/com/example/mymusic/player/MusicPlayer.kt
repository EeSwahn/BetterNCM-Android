package com.example.mymusic.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.example.mymusic.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null,
    val playUrl: String? = null
)

object MusicPlayer {

    private var _exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null

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
                    _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        progressJob?.cancel()
                        progressJob = CoroutineScope(Dispatchers.Main).launch {
                            while (true) {
                                _playerState.value = _playerState.value.copy(
                                    currentPosition = currentPosition
                                )
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
                            _playerState.value = _playerState.value.copy(isLoading = true)
                        }
                        Player.STATE_READY -> {
                            _playerState.value = _playerState.value.copy(
                                isLoading = false,
                                duration = duration
                            )
                        }
                        Player.STATE_ENDED -> {
                            _playerState.value = _playerState.value.copy(isPlaying = false)
                        }
                        Player.STATE_IDLE -> {
                            _playerState.value = _playerState.value.copy(isLoading = false)
                        }
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    _playerState.value = _playerState.value.copy(
                        isLoading = false,
                        isPlaying = false,
                        error = "播放错误: ${error.message}"
                    )
                }
            })
        }
    }

    fun playSong(song: Song, url: String) {
        val player = _exoPlayer ?: return
        _playerState.value = _playerState.value.copy(
            currentSong = song,
            isLoading = true,
            error = null,
            playUrl = url
        )
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
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
        _playerState.value = _playerState.value.copy(isPlaying = false)
    }

    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
    }

    fun clearError() {
        _playerState.value = _playerState.value.copy(error = null)
    }
}
