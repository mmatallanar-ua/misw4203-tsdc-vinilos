package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.PerformerKind
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
import com.misw4203.vinilos.domain.usecase.RemoveAlbumFromCollectorUseCase
import com.misw4203.vinilos.domain.usecase.RemoveFavoriteBandUseCase
import com.misw4203.vinilos.domain.usecase.RemoveFavoriteMusicianUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CollectorDetailUiState {
    data object Loading : CollectorDetailUiState
    data class Success(val collector: CollectorDetail) : CollectorDetailUiState
    data object NotFound : CollectorDetailUiState
    data class Error(val isNetworkError: Boolean) : CollectorDetailUiState
}

sealed interface CollectorDetailEvent {
    data class Removed(val name: String) : CollectorDetailEvent
    data class RemoveFailed(val isNetworkError: Boolean) : CollectorDetailEvent
}

@HiltViewModel
class CollectorDetailViewModel @Inject constructor(
    private val getCollectorDetail: GetCollectorDetailUseCase,
    private val removeFavoriteMusician: RemoveFavoriteMusicianUseCase,
    private val removeFavoriteBand: RemoveFavoriteBandUseCase,
    private val removeAlbumFromCollector: RemoveAlbumFromCollectorUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectorId: Int = savedStateHandle[Destinations.CollectorDetailArg]
        ?: error("collectorId required")

    private val _uiState = MutableStateFlow<CollectorDetailUiState>(CollectorDetailUiState.Loading)
    val uiState: StateFlow<CollectorDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CollectorDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CollectorDetailEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    fun removeFavorite(performer: Performer) {
        val current = (_uiState.value as? CollectorDetailUiState.Success)?.collector ?: return
        // Optimistic removal; restore from network if the call fails.
        _uiState.value = CollectorDetailUiState.Success(
            current.copy(
                favoritePerformers = current.favoritePerformers.filterNot {
                    it.id == performer.id && it.kind == performer.kind
                },
            ),
        )
        // Type undetermined: refuse rather than hit the wrong endpoint.
        if (performer.kind == PerformerKind.UNKNOWN) {
            _uiState.value = CollectorDetailUiState.Success(current)
            _events.tryEmit(CollectorDetailEvent.RemoveFailed(isNetworkError = false))
            return
        }
        viewModelScope.launch {
            when (runCatchingDomain {
                when (performer.kind) {
                    PerformerKind.BAND ->
                        removeFavoriteBand(collectorId, performer.id.toInt())
                    PerformerKind.MUSICIAN ->
                        removeFavoriteMusician(collectorId, performer.id.toInt())
                    PerformerKind.UNKNOWN -> Unit // unreachable; guard above handles this
                }
            }) {
                is DomainResult.Ok   -> _events.tryEmit(CollectorDetailEvent.Removed(performer.name))
                DomainResult.Network -> restore(current, isNetworkError = true)
                DomainResult.NotFound -> restore(current, isNetworkError = false)
                DomainResult.Server  -> restore(current, isNetworkError = false)
            }
        }
    }

    fun removeAlbum(collectorAlbum: CollectorAlbum) {
        val current = (_uiState.value as? CollectorDetailUiState.Success)?.collector ?: return
        val albumId = collectorAlbum.album?.id ?: return
        _uiState.value = CollectorDetailUiState.Success(
            current.copy(
                collectorAlbums = current.collectorAlbums.filterNot { it.id == collectorAlbum.id },
            ),
        )
        viewModelScope.launch {
            when (runCatchingDomain { removeAlbumFromCollector(collectorId, albumId.toInt()) }) {
                is DomainResult.Ok   -> _events.tryEmit(
                    CollectorDetailEvent.Removed(collectorAlbum.album?.name.orEmpty()),
                )
                DomainResult.Network -> restore(current, isNetworkError = true)
                DomainResult.NotFound -> restore(current, isNetworkError = false)
                DomainResult.Server  -> restore(current, isNetworkError = false)
            }
        }
    }

    private fun restore(collector: CollectorDetail, isNetworkError: Boolean) {
        _uiState.value = CollectorDetailUiState.Success(collector)
        _events.tryEmit(CollectorDetailEvent.RemoveFailed(isNetworkError))
    }

    private fun load() {
        _uiState.value = CollectorDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val r = runCatchingDomain { getCollectorDetail(collectorId) }) {
                is DomainResult.Ok -> CollectorDetailUiState.Success(r.value)
                DomainResult.Network -> CollectorDetailUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> CollectorDetailUiState.NotFound
                DomainResult.Server -> CollectorDetailUiState.Error(isNetworkError = false)
            }
        }
    }
}
