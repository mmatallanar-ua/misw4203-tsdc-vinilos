package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.usecase.AddAlbumToCollectorUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
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
class AddAlbumToCollectorViewModel @Inject constructor(
    private val getAlbums: GetAlbumsUseCase,
    private val getCollectorDetail: GetCollectorDetailUseCase,
    private val addAlbumToCollector: AddAlbumToCollectorUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val collectorId: Int = checkNotNull(savedStateHandle.get<Int>(Destinations.AddAlbumCollectorArg)) {
        "AddAlbumToCollectorViewModel requires collectorId in SavedStateHandle"
    }

    private val _form = MutableStateFlow(AddAlbumToCollectorFormState())
    val form: StateFlow<AddAlbumToCollectorFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddAlbumToCollectorUiState>(AddAlbumToCollectorUiState.Loading)
    val uiState: StateFlow<AddAlbumToCollectorUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddAlbumToCollectorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddAlbumToCollectorEvent> = _events.asSharedFlow()

    init {
        loadInitial()
    }

    fun onQueryChange(query: String) {
        _form.value = _form.value.copy(
            query = query,
            filteredAvailable = computeFiltered(_form.value.allAlbums, _form.value.currentAlbumIds, query),
        )
    }

    fun onAlbumSelected(album: Album) {
        _form.value = _form.value.copy(selectedAlbum = album)
    }

    fun onAlbumDeselected() {
        _form.value = _form.value.copy(selectedAlbum = null, price = "", status = "Active")
    }

    fun onPriceChange(value: String) {
        _form.value = _form.value.copy(price = value)
    }

    fun onStatusChange(value: String) {
        _form.value = _form.value.copy(status = value)
    }

    fun onConfirm() {
        val f = _form.value
        if (!f.isFormReady || _uiState.value is AddAlbumToCollectorUiState.Adding) return
        val album = f.selectedAlbum ?: return
        _uiState.value = AddAlbumToCollectorUiState.Adding(album.id.toInt())
        viewModelScope.launch {
            try {
                addAlbumToCollector(collectorId, album.id.toInt(), f.price.toDouble(), f.status)
                val newIds = f.currentAlbumIds + album.id
                _form.value = f.copy(
                    currentAlbumIds = newIds,
                    filteredAvailable = computeFiltered(f.allAlbums, newIds, f.query),
                    selectedAlbum = null,
                    price = "",
                    status = "Active",
                )
                _uiState.value = AddAlbumToCollectorUiState.Ready
                _events.tryEmit(AddAlbumToCollectorEvent.AddedSuccessfully(album.name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddAlbumToCollectorUiState.Error(isNetworkError = true, albumId = album.id.toInt())
                _events.tryEmit(AddAlbumToCollectorEvent.AddFailed(isNetworkError = true))
            } catch (e: HttpException) {
                _uiState.value = AddAlbumToCollectorUiState.Error(isNetworkError = false, albumId = album.id.toInt())
                _events.tryEmit(AddAlbumToCollectorEvent.AddFailed(isNetworkError = false))
            } catch (e: Exception) {
                _uiState.value = AddAlbumToCollectorUiState.Error(isNetworkError = false, albumId = album.id.toInt())
                _events.tryEmit(AddAlbumToCollectorEvent.AddFailed(isNetworkError = false))
            }
        }
    }

    fun retry() {
        loadInitial()
    }

    private fun loadInitial() {
        _uiState.value = AddAlbumToCollectorUiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val albumsAsync = async { getAlbums() }
                    val collectorAsync = async { getCollectorDetail(collectorId) }
                    val albums = albumsAsync.await()
                    val collector = collectorAsync.await()
                    val currentIds = collector.collectorAlbums.mapNotNull { it.album?.id }.toSet()
                    _form.value = AddAlbumToCollectorFormState(
                        allAlbums = albums,
                        currentAlbumIds = currentIds,
                        filteredAvailable = computeFiltered(albums, currentIds, ""),
                    )
                    _uiState.value = AddAlbumToCollectorUiState.Ready
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddAlbumToCollectorUiState.Error(isNetworkError = true, albumId = null)
            } catch (e: HttpException) {
                _uiState.value = AddAlbumToCollectorUiState.Error(isNetworkError = false, albumId = null)
            } catch (e: Exception) {
                _uiState.value = AddAlbumToCollectorUiState.Error(isNetworkError = false, albumId = null)
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
