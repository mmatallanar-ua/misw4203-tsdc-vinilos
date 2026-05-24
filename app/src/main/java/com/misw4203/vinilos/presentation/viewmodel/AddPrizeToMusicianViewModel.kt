package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.usecase.AddPrizeToMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

sealed interface AddPrizeToMusicianEvent {
    data class AddedSuccessfully(val prizeName: String) : AddPrizeToMusicianEvent
    data class AddFailed(val isNetworkError: Boolean) : AddPrizeToMusicianEvent
}

@HiltViewModel
class AddPrizeToMusicianViewModel @Inject constructor(
    private val getMusicianDetail: GetMusicianDetailUseCase,
    private val getPrizes: GetPrizesUseCase,
    private val addPrizeToMusician: AddPrizeToMusicianUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val musicianId: Int = checkNotNull(savedStateHandle.get<Int>(Destinations.AddPrizeMusicianArg)) {
        "AddPrizeToMusicianViewModel requires musicianId in SavedStateHandle"
    }

    private val _form = MutableStateFlow(AddPrizeToMusicianFormState())
    val form: StateFlow<AddPrizeToMusicianFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddPrizeToMusicianUiState>(AddPrizeToMusicianUiState.Loading)
    val uiState: StateFlow<AddPrizeToMusicianUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddPrizeToMusicianEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddPrizeToMusicianEvent> = _events.asSharedFlow()

    init {
        loadInitial()
    }

    fun onQueryChange(query: String) {
        _form.value = _form.value.copy(
            query = query,
            filteredPrizes = filterPrizes(_form.value.allPrizes, query),
        )
    }

    fun onPrizeSelected(prize: Prize) {
        val current = _form.value.selectedPrize
        _form.value = _form.value.copy(
            selectedPrize = if (current?.id == prize.id) null else prize,
            dateError = null,
        )
    }

    fun onDateSelected(millis: Long) {
        val iso = millisToIso(millis)
        val isFuture = millis > System.currentTimeMillis()
        _form.value = _form.value.copy(
            premiationDate = iso,
            dateError = if (isFuture) DateValidationError.FUTURE else null,
        )
    }

    fun onConfirm() {
        if (_uiState.value is AddPrizeToMusicianUiState.Adding) return
        val prize = _form.value.selectedPrize ?: return
        val premiationDate = _form.value.premiationDate ?: return

        if (_form.value.dateError != null) return

        val isDuplicate = _form.value.currentPrizes.any {
            it.id == prize.id && it.premiationDate.take(10) == premiationDate.take(10)
        }
        if (isDuplicate) {
            _form.value = _form.value.copy(dateError = DateValidationError.DUPLICATE)
            return
        }

        _uiState.value = AddPrizeToMusicianUiState.Adding(prize.id)
        viewModelScope.launch {
            when (runCatchingDomain { addPrizeToMusician(musicianId, prize.id, premiationDate) }) {
                is DomainResult.Ok -> {
                    val newPrize = MusicianPrize(
                        id = prize.id,
                        name = prize.name,
                        organization = prize.organization,
                        description = prize.description,
                        premiationDate = premiationDate,
                    )
                    _form.value = _form.value.copy(
                        currentPrizes = _form.value.currentPrizes + newPrize,
                        selectedPrize = null,
                        premiationDate = null,
                        dateError = null,
                    )
                    _uiState.value = AddPrizeToMusicianUiState.Ready
                    _events.tryEmit(AddPrizeToMusicianEvent.AddedSuccessfully(prize.name))
                }
                DomainResult.Network -> {
                    _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = true, prizeId = prize.id)
                    _events.tryEmit(AddPrizeToMusicianEvent.AddFailed(isNetworkError = true))
                }
                DomainResult.NotFound -> {
                    _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false, prizeId = prize.id)
                    _events.tryEmit(AddPrizeToMusicianEvent.AddFailed(isNetworkError = false))
                }
                DomainResult.Server -> {
                    _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false, prizeId = prize.id)
                    _events.tryEmit(AddPrizeToMusicianEvent.AddFailed(isNetworkError = false))
                }
            }
        }
    }

    fun retry() { loadInitial() }

    private fun loadInitial() {
        _uiState.value = AddPrizeToMusicianUiState.Loading
        viewModelScope.launch {
            when (runCatchingDomain {
                coroutineScope {
                    val musicianAsync = async { getMusicianDetail(musicianId) }
                    val prizesAsync = async { getPrizes() }
                    val musician = musicianAsync.await()
                    val prizes = prizesAsync.await()
                    _form.value = AddPrizeToMusicianFormState(
                        allPrizes = prizes,
                        filteredPrizes = prizes,
                        currentPrizes = musician.prizes,
                        musicianName = musician.name,
                        musicianImage = musician.image,
                    )
                    _uiState.value = AddPrizeToMusicianUiState.Ready
                }
            }) {
                is DomainResult.Ok -> Unit
                DomainResult.Network -> _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = true)
                DomainResult.NotFound -> _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false)
                DomainResult.Server -> _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false)
            }
        }
    }

    private fun filterPrizes(prizes: List<Prize>, query: String): List<Prize> {
        if (query.isBlank()) return prizes
        val normalized = normalize(query)
        return prizes.filter {
            normalize(it.name).contains(normalized) || normalize(it.organization).contains(normalized)
        }
    }

    private fun normalize(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }

    private fun millisToIso(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}
