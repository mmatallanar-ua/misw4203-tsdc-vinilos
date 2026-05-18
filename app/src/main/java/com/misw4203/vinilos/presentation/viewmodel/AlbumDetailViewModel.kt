package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.domain.usecase.RemoveCommentUseCase
import com.misw4203.vinilos.domain.usecase.RemoveTrackUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    // Stable ordering anchors: id -> original index in the loaded list.
    // Updated only when the album loads (not on optimistic mutations).
    private var trackOrder: Map<Long, Int> = emptyMap()
    private var commentOrder: Map<Long, Int> = emptyMap()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun removeTrack(track: Track) {
        // update {} runs its lambda synchronously on this thread (re-running it
        // only on CAS contention), so `removed` is settled before we read it below.
        var removed = false
        _uiState.update { state ->
            val success = state as? AlbumDetailUiState.Success ?: return@update state
            if (success.album.tracks.none { it.id == track.id }) return@update state
            removed = true
            AlbumDetailUiState.Success(
                success.album.copy(tracks = success.album.tracks.filterNot { it.id == track.id }),
            )
        }
        if (!removed) return
        viewModelScope.launch {
            try {
                removeTrackUseCase(albumId, track.id)
                _events.tryEmit(AlbumDetailEvent.Removed(track.name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                restoreTrack(track, isNetworkError = false)
            } catch (e: IOException) {
                restoreTrack(track, isNetworkError = true)
            } catch (e: Exception) {
                restoreTrack(track, isNetworkError = false)
            }
        }
    }

    fun removeComment(comment: Comment) {
        // See removeTrack: the update {} lambda is synchronous, so `removed`
        // is settled before the early-return check.
        var removed = false
        _uiState.update { state ->
            val success = state as? AlbumDetailUiState.Success ?: return@update state
            if (success.album.comments.none { it.id == comment.id }) return@update state
            removed = true
            AlbumDetailUiState.Success(
                success.album.copy(comments = success.album.comments.filterNot { it.id == comment.id }),
            )
        }
        if (!removed) return
        viewModelScope.launch {
            try {
                removeCommentUseCase(albumId, comment.id)
                _events.tryEmit(AlbumDetailEvent.Removed(comment.description))
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                restoreComment(comment, isNetworkError = false)
            } catch (e: IOException) {
                restoreComment(comment, isNetworkError = true)
            } catch (e: Exception) {
                restoreComment(comment, isNetworkError = false)
            }
        }
    }

    private fun restoreTrack(track: Track, isNetworkError: Boolean) {
        _uiState.update { state ->
            (state as? AlbumDetailUiState.Success)
                ?.takeIf { s -> s.album.tracks.none { it.id == track.id } }
                ?.let { s ->
                    val list = s.album.tracks.toMutableList()
                    val insertAt = insertionIndex(track.id, list.map(Track::id), trackOrder)
                    list.add(insertAt, track)
                    AlbumDetailUiState.Success(s.album.copy(tracks = list))
                } ?: state
        }
        _events.tryEmit(AlbumDetailEvent.RemoveFailed(isNetworkError))
    }

    private fun restoreComment(comment: Comment, isNetworkError: Boolean) {
        _uiState.update { state ->
            (state as? AlbumDetailUiState.Success)
                ?.takeIf { s -> s.album.comments.none { it.id == comment.id } }
                ?.let { s ->
                    val list = s.album.comments.toMutableList()
                    val insertAt = insertionIndex(comment.id, list.map(Comment::id), commentOrder)
                    list.add(insertAt, comment)
                    AlbumDetailUiState.Success(s.album.copy(comments = list))
                } ?: state
        }
        _events.tryEmit(AlbumDetailEvent.RemoveFailed(isNetworkError))
    }

    /**
     * Finds the insertion index that preserves the original ordering.
     * Counts how many ids currently in [currentIds] had a smaller original index
     * than [itemId]. The result is how many current items should precede [itemId].
     */
    private fun insertionIndex(
        itemId: Long,
        currentIds: List<Long>,
        order: Map<Long, Int>,
    ): Int {
        val originalIndex = order[itemId] ?: return currentIds.size
        return currentIds.count { id -> (order[id] ?: Int.MAX_VALUE) < originalIndex }
    }

    private fun load() {
        _uiState.value = AlbumDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getAlbumDetail(albumId) }) {
                is DomainResult.Ok -> {
                    val detail = r.value
                    trackOrder = detail.tracks.mapIndexed { idx, t -> t.id to idx }.toMap()
                    commentOrder = detail.comments.mapIndexed { idx, c -> c.id to idx }.toMap()
                    AlbumDetailUiState.Success(detail)
                }
                DomainResult.Network -> AlbumDetailUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> AlbumDetailUiState.NotFound
                DomainResult.Server -> AlbumDetailUiState.Error(isNetworkError = false)
            }
        }
    }
}
