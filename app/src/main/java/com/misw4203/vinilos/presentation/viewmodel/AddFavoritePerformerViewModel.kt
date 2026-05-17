package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.usecase.AddFavoriteBandUseCase
import com.misw4203.vinilos.domain.usecase.AddFavoriteMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import com.misw4203.vinilos.domain.usecase.GetCollectorDetailUseCase
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
class AddFavoritePerformerViewModel @Inject constructor(
    private val getMusicians: GetMusiciansUseCase,
    private val getBands: GetBandsUseCase,
    private val getCollectorDetail: GetCollectorDetailUseCase,
    private val addFavoriteMusician: AddFavoriteMusicianUseCase,
    private val addFavoriteBand: AddFavoriteBandUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val collectorId: Int = checkNotNull(
        savedStateHandle.get<Int>(Destinations.AddFavoritePerformerCollectorArg),
    ) { "AddFavoritePerformerViewModel requires collectorId in SavedStateHandle" }

    private val _form = MutableStateFlow(AddFavoritePerformerFormState())
    val form: StateFlow<AddFavoritePerformerFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddFavoritePerformerUiState>(AddFavoritePerformerUiState.Loading)
    val uiState: StateFlow<AddFavoritePerformerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddFavoritePerformerEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddFavoritePerformerEvent> = _events.asSharedFlow()

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
            filteredMusicians = filterMusicians(current.allMusicians, current.favoriteMusicianIds, query),
            filteredBands = filterBands(current.allBands, current.favoriteBandIds, query),
        )
    }

    fun onAddMusician(musicianId: Int) {
        if (_uiState.value is AddFavoritePerformerUiState.Adding) return
        _uiState.value = AddFavoritePerformerUiState.Adding(PerformerType.MUSICIAN, musicianId)
        viewModelScope.launch {
            try {
                addFavoriteMusician(collectorId, musicianId)
                val musician = _form.value.allMusicians.firstOrNull { it.id == musicianId }
                val newFavorites = _form.value.favoriteMusicianIds + musicianId
                _form.value = _form.value.copy(
                    favoriteMusicianIds = newFavorites,
                    filteredMusicians = filterMusicians(
                        _form.value.allMusicians,
                        newFavorites,
                        _form.value.query,
                    ),
                )
                _uiState.value = AddFavoritePerformerUiState.Ready
                if (musician != null) {
                    _events.tryEmit(AddFavoritePerformerEvent.AddedSuccessfully(musician.name))
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
        if (_uiState.value is AddFavoritePerformerUiState.Adding) return
        _uiState.value = AddFavoritePerformerUiState.Adding(PerformerType.BAND, bandId)
        viewModelScope.launch {
            try {
                addFavoriteBand(collectorId, bandId)
                val band = _form.value.allBands.firstOrNull { it.id == bandId }
                val newFavorites = _form.value.favoriteBandIds + bandId
                _form.value = _form.value.copy(
                    favoriteBandIds = newFavorites,
                    filteredBands = filterBands(
                        _form.value.allBands,
                        newFavorites,
                        _form.value.query,
                    ),
                )
                _uiState.value = AddFavoritePerformerUiState.Ready
                if (band != null) {
                    _events.tryEmit(AddFavoritePerformerEvent.AddedSuccessfully(band.name))
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
        _uiState.value = AddFavoritePerformerUiState.Error(
            isNetworkError = isNetwork,
            type = type,
            performerId = id,
        )
        _events.tryEmit(AddFavoritePerformerEvent.AddFailed(isNetworkError = isNetwork))
    }

    private fun loadInitial() {
        _uiState.value = AddFavoritePerformerUiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val musiciansAsync = async { getMusicians() }
                    val bandsAsync = async { getBands() }
                    val collectorAsync = async { getCollectorDetail(collectorId) }
                    val musicians = musiciansAsync.await()
                    val bands = bandsAsync.await()
                    val collector = collectorAsync.await()
                    // favoritePerformers mezcla musicos y bandas sin distinguir tipo:
                    // usamos exclusion ciega por id contra ambas listas.
                    val favoriteIds = collector.favoritePerformers.map { it.id.toInt() }.toSet()
                    val favoriteMusicianIds = musicians.map { it.id }.filter { it in favoriteIds }.toSet()
                    val favoriteBandIds = bands.map { it.id }.filter { it in favoriteIds }.toSet()
                    _form.value = AddFavoritePerformerFormState(
                        query = "",
                        collectorName = collector.name,
                        selectedTab = PerformerType.MUSICIAN,
                        allMusicians = musicians,
                        allBands = bands,
                        favoriteMusicianIds = favoriteMusicianIds,
                        favoriteBandIds = favoriteBandIds,
                        filteredMusicians = filterMusicians(musicians, favoriteMusicianIds, ""),
                        filteredBands = filterBands(bands, favoriteBandIds, ""),
                    )
                    _uiState.value = AddFavoritePerformerUiState.Ready
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddFavoritePerformerUiState.Error(
                    isNetworkError = true, type = null, performerId = null,
                )
            } catch (e: HttpException) {
                _uiState.value = AddFavoritePerformerUiState.Error(
                    isNetworkError = false, type = null, performerId = null,
                )
            } catch (e: Exception) {
                _uiState.value = AddFavoritePerformerUiState.Error(
                    isNetworkError = false, type = null, performerId = null,
                )
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
        val normalizedQuery = normalize(query)
        return available.filter { normalize(it.name).contains(normalizedQuery) }
    }

    private fun filterBands(
        all: List<BandSummary>,
        excluded: Set<Int>,
        query: String,
    ): List<BandSummary> {
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
