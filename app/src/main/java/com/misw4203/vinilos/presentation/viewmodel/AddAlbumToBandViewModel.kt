package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.usecase.AddAlbumToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class AddAlbumToBandViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase,
    private val getBandDetail: GetBandDetailUseCase,
    private val addAlbumToBand: AddAlbumToBandUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val bandId: Int = checkNotNull(savedStateHandle.get<Int>(Destinations.AddAlbumBandArg)) {
        "AddAlbumToBandViewModel requires bandId in SavedStateHandle"
    }

    private val _form = MutableStateFlow(AddAlbumToBandFormState())
    val form: StateFlow<AddAlbumToBandFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddAlbumToBandUiState>(AddAlbumToBandUiState.Loading)
    val uiState: StateFlow<AddAlbumToBandUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddAlbumToBandEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddAlbumToBandEvent> = _events.asSharedFlow()

    init {
        loadInitial()
    }

    fun onQueryChange(query: String) {
        _form.value = _form.value.copy(
            query = query,
            filteredAvailable = computeFiltered(_form.value.allAlbums, _form.value.currentAlbumIds, query),
        )
    }

    fun onAddAlbum(albumId: Int) {
        if (_uiState.value is AddAlbumToBandUiState.Adding) return
        _uiState.value = AddAlbumToBandUiState.Adding(albumId)
        viewModelScope.launch {
            try {
                addAlbumToBand(bandId, albumId.toLong())
                val album = _form.value.allAlbums.firstOrNull { it.id == albumId.toLong() }
                val newIds = _form.value.currentAlbumIds + albumId.toLong()
                _form.value = _form.value.copy(
                    currentAlbumIds = newIds,
                    filteredAvailable = computeFiltered(_form.value.allAlbums, newIds, _form.value.query),
                )
                _uiState.value = AddAlbumToBandUiState.Ready
                if (album != null) {
                    _events.tryEmit(AddAlbumToBandEvent.AddedSuccessfully(album.name))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddAlbumToBandUiState.Error(isNetworkError = true, albumId = albumId)
                _events.tryEmit(AddAlbumToBandEvent.AddFailed(isNetworkError = true))
            } catch (e: HttpException) {
                _uiState.value = AddAlbumToBandUiState.Error(isNetworkError = false, albumId = albumId)
                _events.tryEmit(AddAlbumToBandEvent.AddFailed(isNetworkError = false))
            } catch (e: Exception) {
                _uiState.value = AddAlbumToBandUiState.Error(isNetworkError = false, albumId = albumId)
                _events.tryEmit(AddAlbumToBandEvent.AddFailed(isNetworkError = false))
            }
        }
    }

    fun retry() {
        loadInitial()
    }

    private fun loadInitial() {
        _uiState.value = AddAlbumToBandUiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val albumsAsync = async { getAlbums() }
                    val bandAsync = async { getBandDetail(bandId) }
                    val albums = albumsAsync.await()
                    val band = bandAsync.await()
                    val currentIds = band.albums.map { it.id }.toSet()
                    _form.value = AddAlbumToBandFormState(
                        allAlbums = albums,
                        currentAlbumIds = currentIds,
                        filteredAvailable = computeFiltered(albums, currentIds, ""),
                    )
                    _uiState.value = AddAlbumToBandUiState.Ready
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddAlbumToBandUiState.Error(isNetworkError = true, albumId = null)
            } catch (e: HttpException) {
                _uiState.value = AddAlbumToBandUiState.Error(isNetworkError = false, albumId = null)
            } catch (e: Exception) {
                _uiState.value = AddAlbumToBandUiState.Error(isNetworkError = false, albumId = null)
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
