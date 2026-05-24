package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.usecase.AddAlbumToMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class AddAlbumToMusicianViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase,
    private val getMusicianDetail: GetMusicianDetailUseCase,
    private val addAlbumToMusician: AddAlbumToMusicianUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val musicianId: Int = checkNotNull(savedStateHandle.get<Int>(Destinations.AddAlbumMusicianArg)) {
        "AddAlbumToMusicianViewModel requires musicianId in SavedStateHandle"
    }

    private val _form = MutableStateFlow(AddAlbumToMusicianFormState())
    val form: StateFlow<AddAlbumToMusicianFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddAlbumToMusicianUiState>(AddAlbumToMusicianUiState.Loading)
    val uiState: StateFlow<AddAlbumToMusicianUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddAlbumToMusicianEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddAlbumToMusicianEvent> = _events.asSharedFlow()

    init {
        loadInitial()
    }

    fun onQueryChange(query: String) {
        _form.value = _form.value.copy(
            query = query,
            filteredAvailable = computeFiltered(_form.value.allAlbums, _form.value.currentAlbumIds, query),
        )
    }

    fun onAddAlbum(albumId: Long) {
        if (_uiState.value is AddAlbumToMusicianUiState.Adding) return
        _uiState.value = AddAlbumToMusicianUiState.Adding(albumId)
        viewModelScope.launch {
            when (runCatchingDomain { addAlbumToMusician(musicianId, albumId) }) {
                is DomainResult.Ok -> {
                    val album = _form.value.allAlbums.firstOrNull { it.id == albumId }
                    val newIds = _form.value.currentAlbumIds + albumId
                    _form.value = _form.value.copy(
                        currentAlbumIds = newIds,
                        filteredAvailable = computeFiltered(_form.value.allAlbums, newIds, _form.value.query),
                    )
                    _uiState.value = AddAlbumToMusicianUiState.Ready
                    if (album != null) {
                        _events.tryEmit(AddAlbumToMusicianEvent.AddedSuccessfully(album.name))
                    }
                }
                DomainResult.Network -> {
                    _uiState.value = AddAlbumToMusicianUiState.Error(isNetworkError = true, albumId = albumId)
                    _events.tryEmit(AddAlbumToMusicianEvent.AddFailed(isNetworkError = true))
                }
                DomainResult.NotFound -> {
                    _uiState.value = AddAlbumToMusicianUiState.Error(isNetworkError = false, albumId = albumId)
                    _events.tryEmit(AddAlbumToMusicianEvent.AddFailed(isNetworkError = false))
                }
                DomainResult.Server -> {
                    _uiState.value = AddAlbumToMusicianUiState.Error(isNetworkError = false, albumId = albumId)
                    _events.tryEmit(AddAlbumToMusicianEvent.AddFailed(isNetworkError = false))
                }
            }
        }
    }

    fun retry() {
        loadInitial()
    }

    private fun loadInitial() {
        _uiState.value = AddAlbumToMusicianUiState.Loading
        viewModelScope.launch {
            when (runCatchingDomain {
                coroutineScope {
                    val albumsAsync = async { getAlbums() }
                    val musicianAsync = async { getMusicianDetail(musicianId) }
                    val albums = albumsAsync.await()
                    val musician = musicianAsync.await()
                    val currentIds = musician.albums.map { it.id }.toSet()
                    _form.value = AddAlbumToMusicianFormState(
                        allAlbums = albums,
                        currentAlbumIds = currentIds,
                        filteredAvailable = computeFiltered(albums, currentIds, ""),
                    )
                    _uiState.value = AddAlbumToMusicianUiState.Ready
                }
            }) {
                is DomainResult.Ok -> Unit
                DomainResult.Network -> _uiState.value = AddAlbumToMusicianUiState.Error(isNetworkError = true, albumId = null)
                DomainResult.NotFound -> _uiState.value = AddAlbumToMusicianUiState.Error(isNetworkError = false, albumId = null)
                DomainResult.Server -> _uiState.value = AddAlbumToMusicianUiState.Error(isNetworkError = false, albumId = null)
            }
        }
    }

    private fun computeFiltered(all: List<Album>, excluded: Set<Long>, query: String): List<Album> {
        val available = all.filterNot { it.id in excluded }
        if (query.isBlank()) return available
        val normalizedQuery = normalize(query)
        return available.filter { normalize(it.name).contains(normalizedQuery) }
    }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }
}
