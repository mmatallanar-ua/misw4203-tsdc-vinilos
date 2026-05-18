package com.misw4203.vinilos.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.NewTrack
import com.misw4203.vinilos.domain.usecase.AddTrackUseCase
import com.misw4203.vinilos.presentation.common.DomainFailure
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

@HiltViewModel
class AddTrackViewModel @Inject constructor(
    private val addTrack: AddTrackUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle[Destinations.AddTrackAlbumArg])

    var name by mutableStateOf("")
    var nameError by mutableStateOf<String?>(null)

    var duration by mutableStateOf("")
    var durationError by mutableStateOf<String?>(null)

    private val _uiState = MutableStateFlow<AddTrackUiState>(AddTrackUiState.Idle)
    val uiState: StateFlow<AddTrackUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddTrackEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddTrackEvent> = _events.asSharedFlow()

    fun submit() {
        nameError = if (name.isBlank()) "El nombre del track es obligatorio" else null
        durationError = if (!isValidDuration(duration)) "Formato: MM:SS (ej: 03:45)" else null

        if (nameError != null || durationError != null) return

        _uiState.value = AddTrackUiState.Submitting
        viewModelScope.launch {
            when (runCatchingDomain { addTrack(albumId, NewTrack(name.trim(), duration.trim())) }) {
                is DomainResult.Ok -> {
                    _uiState.value = AddTrackUiState.Idle
                    _events.tryEmit(AddTrackEvent.Submitted)
                }
                DomainResult.Network -> _uiState.value = AddTrackUiState.Error(DomainFailure.NETWORK)
                DomainResult.NotFound -> _uiState.value = AddTrackUiState.Error(DomainFailure.NOT_FOUND)
                DomainResult.Server -> _uiState.value = AddTrackUiState.Error(DomainFailure.SERVER)
            }
        }
    }

    fun resetError() {
        if (_uiState.value is AddTrackUiState.Error) {
            _uiState.value = AddTrackUiState.Idle
        }
    }

    internal fun isValidDuration(value: String): Boolean {
        val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(value) ?: return false
        return match.groupValues[2].toInt() < 60
    }
}
