package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.domain.usecase.RemoveCommentUseCase
import com.misw4203.vinilos.domain.usecase.RemoveTrackUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

sealed interface AlbumDetailEvent {
    data class Removed(val name: String) : AlbumDetailEvent
    data class RemoveFailed(val isNetworkError: Boolean) : AlbumDetailEvent
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumDetail: GetAlbumDetailUseCase,
    private val removeTrackUseCase: RemoveTrackUseCase,
    private val removeCommentUseCase: RemoveCommentUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle[Destinations.AlbumDetailArg])

    private val _uiState = MutableStateFlow<AlbumDetailUiState>(AlbumDetailUiState.Loading)
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AlbumDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AlbumDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun removeTrack(track: Track) {
        val current = (_uiState.value as? AlbumDetailUiState.Success)?.album ?: return
        _uiState.value = AlbumDetailUiState.Success(
            current.copy(tracks = current.tracks.filterNot { it.id == track.id }),
        )
        viewModelScope.launch {
            try {
                removeTrackUseCase(albumId, track.id)
                _events.tryEmit(AlbumDetailEvent.Removed(track.name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                restore(current, isNetworkError = false)
            } catch (e: IOException) {
                restore(current, isNetworkError = true)
            } catch (e: Exception) {
                restore(current, isNetworkError = false)
            }
        }
    }

    fun removeComment(comment: Comment) {
        val current = (_uiState.value as? AlbumDetailUiState.Success)?.album ?: return
        _uiState.value = AlbumDetailUiState.Success(
            current.copy(comments = current.comments.filterNot { it.id == comment.id }),
        )
        viewModelScope.launch {
            try {
                removeCommentUseCase(albumId, comment.id)
                _events.tryEmit(AlbumDetailEvent.Removed(comment.description))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                restore(current, isNetworkError = false)
            } catch (e: IOException) {
                restore(current, isNetworkError = true)
            } catch (e: Exception) {
                restore(current, isNetworkError = false)
            }
        }
    }

    private fun restore(album: AlbumDetail, isNetworkError: Boolean) {
        _uiState.value = AlbumDetailUiState.Success(album)
        _events.tryEmit(AlbumDetailEvent.RemoveFailed(isNetworkError))
    }

    private fun load() {
        _uiState.value = AlbumDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                AlbumDetailUiState.Success(getAlbumDetail(albumId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                if (e.code() == 404) AlbumDetailUiState.NotFound
                else AlbumDetailUiState.Error(isNetworkError = false)
            } catch (e: IOException) {
                AlbumDetailUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                AlbumDetailUiState.Error(isNetworkError = false)
            }
        }
    }
}
