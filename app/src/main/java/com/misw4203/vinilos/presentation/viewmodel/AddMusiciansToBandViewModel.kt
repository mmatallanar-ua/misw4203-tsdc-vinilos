package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.usecase.AddMusicianToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.text.Normalizer
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AddMusiciansToBandViewModel @Inject constructor(
    private val getMusicians: GetMusiciansUseCase,
    private val getBandDetail: GetBandDetailUseCase,
    private val addMusicianToBand: AddMusicianToBandUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val bandId: Int = checkNotNull(savedStateHandle.get<Int>(Destinations.AddMusiciansBandArg)) {
        "AddMusiciansToBandViewModel requires bandId in SavedStateHandle"
    }

    private val _form = MutableStateFlow(AddMusiciansFormState())
    val form: StateFlow<AddMusiciansFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddMusiciansUiState>(AddMusiciansUiState.Loading)
    val uiState: StateFlow<AddMusiciansUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddMusiciansEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddMusiciansEvent> = _events.asSharedFlow()

    private val queryChannel = MutableStateFlow("")

    init {
        loadInitial()
        queryChannel
            .debounce(300L)
            .distinctUntilChanged()
            .onEach { q ->
                _form.value = _form.value.copy(
                    query = q,
                    filteredAvailable = computeFiltered(_form.value.allMusicians, _form.value.currentMemberIds, q),
                )
            }
            .launchIn(viewModelScope)
    }

    fun onQueryChange(query: String) {
        queryChannel.value = query
    }

    fun onAddMusician(musicianId: Int) {
        if (_uiState.value is AddMusiciansUiState.Adding) return
        _uiState.value = AddMusiciansUiState.Adding(musicianId)
        viewModelScope.launch {
            try {
                addMusicianToBand(bandId, musicianId)
                val musician = _form.value.allMusicians.firstOrNull { it.id == musicianId }
                _form.value = _form.value.copy(
                    currentMemberIds = _form.value.currentMemberIds + musicianId,
                    filteredAvailable = computeFiltered(
                        _form.value.allMusicians,
                        _form.value.currentMemberIds + musicianId,
                        _form.value.query,
                    ),
                )
                _uiState.value = AddMusiciansUiState.Ready
                if (musician != null) {
                    _events.tryEmit(AddMusiciansEvent.AddedSuccessfully(musician.name))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = true, musicianId = musicianId)
            } catch (e: HttpException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = musicianId)
            } catch (e: Exception) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = musicianId)
            }
        }
    }

    private fun loadInitial() {
        _uiState.value = AddMusiciansUiState.Loading
        viewModelScope.launch {
            try {
                coroutineScope {
                    val musiciansAsync = async { getMusicians() }
                    val bandAsync = async { getBandDetail(bandId) }
                    val musicians = musiciansAsync.await()
                    val band = bandAsync.await()
                    val memberIds = band.members.map { it.id }.toSet()
                    _form.value = AddMusiciansFormState(
                        query = "",
                        allMusicians = musicians,
                        currentMemberIds = memberIds,
                        filteredAvailable = computeFiltered(musicians, memberIds, ""),
                    )
                    _uiState.value = AddMusiciansUiState.Ready
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = true, musicianId = null)
            } catch (e: HttpException) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = null)
            } catch (e: Exception) {
                _uiState.value = AddMusiciansUiState.Error(isNetworkError = false, musicianId = null)
            }
        }
    }

    fun retry() {
        loadInitial()
    }

    private fun computeFiltered(
        all: List<MusicianSummary>,
        excluded: Set<Int>,
        query: String,
    ): List<MusicianSummary> {
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
