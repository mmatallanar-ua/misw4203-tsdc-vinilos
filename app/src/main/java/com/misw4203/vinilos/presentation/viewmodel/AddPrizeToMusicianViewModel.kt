package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.usecase.AddPrizeToMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetPrizesUseCase
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
import java.util.Calendar
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
            yearError = null,
        )
    }

    fun onYearChange(year: String) {
        val error = if (year.isBlank()) null else validateYear(year)
        _form.value = _form.value.copy(year = year, yearError = error)
    }

    fun onConfirm() {
        if (_uiState.value is AddPrizeToMusicianUiState.Adding) return
        val prize = _form.value.selectedPrize ?: return

        val yearError = validateYear(_form.value.year)
        if (yearError != null) {
            _form.value = _form.value.copy(yearError = yearError)
            return
        }

        val year = _form.value.year
        val isDuplicate = _form.value.currentPrizes.any {
            it.id == prize.id && it.premiationDate.take(4) == year
        }
        if (isDuplicate) {
            _form.value = _form.value.copy(yearError = YearValidationError.DUPLICATE)
            return
        }

        val premiationDate = "$year-01-01T00:00:00.000Z"
        _uiState.value = AddPrizeToMusicianUiState.Adding(prize.id)
        viewModelScope.launch {
            try {
                addPrizeToMusician(musicianId, prize.id, premiationDate)
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
                    year = "",
                    yearError = null,
                )
                _uiState.value = AddPrizeToMusicianUiState.Ready
                _events.tryEmit(AddPrizeToMusicianEvent.AddedSuccessfully(prize.name))
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = true, prizeId = prize.id)
                _events.tryEmit(AddPrizeToMusicianEvent.AddFailed(isNetworkError = true))
            } catch (e: HttpException) {
                _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false, prizeId = prize.id)
                _events.tryEmit(AddPrizeToMusicianEvent.AddFailed(isNetworkError = false))
            } catch (e: Exception) {
                _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false, prizeId = prize.id)
                _events.tryEmit(AddPrizeToMusicianEvent.AddFailed(isNetworkError = false))
            }
        }
    }

    fun retry() { loadInitial() }

    private fun loadInitial() {
        _uiState.value = AddPrizeToMusicianUiState.Loading
        viewModelScope.launch {
            try {
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = true)
            } catch (e: HttpException) {
                _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false)
            } catch (e: Exception) {
                _uiState.value = AddPrizeToMusicianUiState.Error(isNetworkError = false)
            }
        }
    }

    private fun validateYear(year: String): YearValidationError? {
        if (year.isBlank()) return YearValidationError.EMPTY
        if (year.length != 4) return YearValidationError.INVALID
        val y = year.toIntOrNull() ?: return YearValidationError.INVALID
        if (y < 1900) return YearValidationError.INVALID
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (y > currentYear) return YearValidationError.FUTURE
        return null
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
}
