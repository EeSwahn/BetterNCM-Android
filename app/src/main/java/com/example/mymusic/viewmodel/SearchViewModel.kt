package com.example.mymusic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymusic.data.model.Song
import com.example.mymusic.data.repository.MusicRepository
import com.example.mymusic.data.repository.Result
import com.example.mymusic.player.MusicPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val songs: List<Song> = emptyList(),
    val hasSearched: Boolean = false,
    val errorMessage: String? = null,
    val totalCount: Int = 0
)

data class PlayingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SearchViewModel : ViewModel() {

    private val repository = MusicRepository()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _playingState = MutableStateFlow(PlayingUiState())
    val playingState: StateFlow<PlayingUiState> = _playingState.asStateFlow()

    private val _logoutEvent = MutableStateFlow(false)
    val logoutEvent: StateFlow<Boolean> = _logoutEvent.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _logoutEvent.value = true
        }
    }

    fun resetLogoutEvent() {
        _logoutEvent.value = false
    }

    fun setQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search(query: String = _uiState.value.query) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                hasSearched = true,
                errorMessage = null
            )
            when (val result = repository.search(query)) {
                is Result.Success -> {
                    val songs = result.data.result?.songs ?: emptyList()
                    val total = result.data.result?.songCount ?: 0
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        songs = songs,
                        totalCount = total
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            _playingState.value = PlayingUiState(isLoading = true)
            when (val result = repository.getSongUrl(song.id)) {
                is Result.Success -> {
                    _playingState.value = PlayingUiState(isLoading = false)
                    MusicPlayer.playSong(song, result.data)
                }
                is Result.Error -> {
                    _playingState.value = PlayingUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
        _playingState.value = _playingState.value.copy(errorMessage = null)
    }

    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            query = "",
            songs = emptyList(),
            hasSearched = false,
            errorMessage = null
        )
    }
}
