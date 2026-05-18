package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.usecase.CreateAlbumUseCase
import com.misw4203.vinilos.presentation.common.DomainFailure
import com.misw4203.vinilos.presentation.common.DomainResult
import com.misw4203.vinilos.presentation.common.runCatchingDomain
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
class CreateAlbumViewModel @Inject constructor(
    private val createAlbum: CreateAlbumUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateAlbumUiState>(CreateAlbumUiState.Idle)
    val uiState: StateFlow<CreateAlbumUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CreateAlbumEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CreateAlbumEvent> = _events.asSharedFlow()

    fun submit(input: CreateAlbumInput) {
        _uiState.value = CreateAlbumUiState.Submitting
        viewModelScope.launch {
            when (runCatchingDomain { createAlbum(input) }) {
                is DomainResult.Ok -> {
                    _uiState.value = CreateAlbumUiState.Idle
                    _events.tryEmit(CreateAlbumEvent.Submitted)
                }
                DomainResult.Network -> _uiState.value = CreateAlbumUiState.Error(DomainFailure.NETWORK)
                DomainResult.NotFound -> _uiState.value = CreateAlbumUiState.Error(DomainFailure.NOT_FOUND)
                DomainResult.Server -> _uiState.value = CreateAlbumUiState.Error(DomainFailure.SERVER)
            }
        }
    }

    fun resetError() {
        if (_uiState.value is CreateAlbumUiState.Error) _uiState.value = CreateAlbumUiState.Idle
    }
}
