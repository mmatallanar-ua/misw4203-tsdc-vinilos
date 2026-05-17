package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.usecase.AddBandToAlbumUseCase
import com.misw4203.vinilos.domain.usecase.AddMusicianToAlbumUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
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
class AddPerformerToAlbumViewModel @Inject constructor(
    private val getMusicians: GetMusiciansUseCase,
    private val getBands: GetBandsUseCase,
    private val getAlbumDetail: GetAlbumDetailUseCase,
    private val addMusicianToAlbum: AddMusicianToAlbumUseCase,
    private val addBandToAlbum: AddBandToAlbumUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: Long = checkNotNull(
        savedStateHandle.get<Long>(Destinations.AddPerformerAlbumArg),
    ) { "AddPerformerToAlbumViewModel requires albumId in SavedStateHandle" }

    private val _form = MutableStateFlow(AddPerformerToAlbumFormState())
    val form: StateFlow<AddPerformerToAlbumFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddPerformerToAlbumUiState>(AddPerformerToAlbumUiState.Loading)
    val uiState: StateFlow<AddPerformerToAlbumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddPerformerToAlbumEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddPerformerToAlbumEvent> = _events.asSharedFlow()

    init {
        loadInitial()
    }

    fun onTabChange(type: PerformerType) {
        _form.value = _form.value.copy(selectedTab = type)
    }

    fun onQueryChange(query: String) {
        val current = _form.value
        _form.value = current.copy(
            query = query,
            filteredMusicians = filterMusicians(current.allMusicians, current.currentMusicianIds, query),
            filteredBands = filterBands(current.allBands, current.currentBandIds, query),
        )
    }

    fun onAddMusician(musicianId: Int) {
        if (_uiState.value is AddPerformerToAlbumUiState.Adding) return
        _uiState.value = AddPerformerToAlbumUiState.Adding(PerformerType.MUSICIAN, musicianId)
        viewModelScope.launch {
            try {
                addMusicianToAlbum(albumId, musicianId)
                val musician = _form.value.allMusicians.firstOrNull { it.id == musicianId }
                val newIds = _form.value.currentMusicianIds + musicianId
                _form.value = _form.value.copy(
                    currentMusicianIds = newIds,
                    filteredMusicians = filterMusicians(_form.value.allMusicians, newIds, _form.value.query),
                )
                _uiState.value = AddPerformerToAlbumUiState.Ready
                if (musician != null) {
                    _events.tryEmit(AddPerformerToAlbumEvent.AddedSuccessfully(musician.name))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                emitAddError(PerformerType.MUSICIAN, musicianId, isNetwork = true)
            } catch (e: HttpException) {
                emitAddError(PerformerType.MUSICIAN, musicianId, isNetwork = false)
            } catch (e: Exception) {
                emitAddError(PerformerType.MUSICIAN, musicianId, isNetwork = false)
            }
        }
    }

    fun onAddBand(bandId: Int) {
        if (_uiState.value is AddPerformerToAlbumUiState.Adding) return
        _uiState.value = AddPerformerToAlbumUiState.Adding(PerformerType.BAND, bandId)
        viewModelScope.launch {
            try {
                addBandToAlbum(albumId, bandId)
                val band = _form.value.allBands.firstOrNull { it.id == bandId }
                val newIds = _form.value.currentBandIds + bandId
                _form.value = _form.value.copy(
                    currentBandIds = newIds,
                    filteredBands = filterBands(_form.value.allBands, newIds, _form.value.query),
                )
                _uiState.value = AddPerformerToAlbumUiState.Ready
                if (band != null) {
                    _events.tryEmit(AddPerformerToAlbumEvent.AddedSuccessfully(band.name))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                emitAddError(PerformerType.BAND, bandId, isNetwork = true)
            } catch (e: HttpException) {
                emitAddError(PerformerType.BAND, bandId, isNetwork = false)
            } catch (e: Exception) {
                emitAddError(PerformerType.BAND, bandId, isNetwork = false)
            }
        }
    }

    fun retry() {
        loadInitial()
    }

    private fun emitAddError(type: PerformerType, id: Int, isNetwork: Boolean) {
        _uiState.value = AddPerformerToAlbumUiState.Error(
            isNetworkError = isNetwork,
            type = type,
            performerId = id,
        )
        _events.tryEmit(AddPerformerToAlbumEvent.AddFailed(isNetworkError = isNetwork))
    }

    private fun loadInitial() {
        _uiState.value = AddPerformerToAlbumUiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val musiciansAsync = async { getMusicians() }
                    val bandsAsync = async { getBands() }
                    val albumAsync = async { getAlbumDetail(albumId) }
                    val musicians = musiciansAsync.await()
                    val bands = bandsAsync.await()
                    val album = albumAsync.await()
                    // performers mix musicians and bands without a type tag; use
                    // blind id exclusion against both lists (same as favorites).
                    val performerIds = album.performers.map { it.id.toInt() }.toSet()
                    val currentMusicianIds = musicians.map { it.id }.filter { it in performerIds }.toSet()
                    val currentBandIds = bands.map { it.id }.filter { it in performerIds }.toSet()
                    _form.value = AddPerformerToAlbumFormState(
                        query = "",
                        albumName = album.name,
                        selectedTab = PerformerType.MUSICIAN,
                        allMusicians = musicians,
                        allBands = bands,
                        currentMusicianIds = currentMusicianIds,
                        currentBandIds = currentBandIds,
                        filteredMusicians = filterMusicians(musicians, currentMusicianIds, ""),
                        filteredBands = filterBands(bands, currentBandIds, ""),
                    )
                    _uiState.value = AddPerformerToAlbumUiState.Ready
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddPerformerToAlbumUiState.Error(true, null, null)
            } catch (e: HttpException) {
                _uiState.value = AddPerformerToAlbumUiState.Error(false, null, null)
            } catch (e: Exception) {
                _uiState.value = AddPerformerToAlbumUiState.Error(false, null, null)
            }
        }
    }

    private fun filterMusicians(
        all: List<MusicianSummary>,
        excluded: Set<Int>,
        query: String,
    ): List<MusicianSummary> {
        val available = all.filterNot { it.id in excluded }
        if (query.isBlank()) return available
        val q = normalize(query)
        return available.filter { normalize(it.name).contains(q) }
    }

    private fun filterBands(
        all: List<BandSummary>,
        excluded: Set<Int>,
        query: String,
    ): List<BandSummary> {
        val available = all.filterNot { it.id in excluded }
        if (query.isBlank()) return available
        val q = normalize(query)
        return available.filter { normalize(it.name).contains(q) }
    }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }
}
