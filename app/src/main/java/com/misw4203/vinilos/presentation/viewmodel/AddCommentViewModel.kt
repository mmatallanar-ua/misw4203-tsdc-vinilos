package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.AddCommentUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCommentViewModel @Inject constructor(
    private val addComment: AddCommentUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle[Destinations.AddCommentAlbumArg])
    private val collectorId: Int = checkNotNull(savedStateHandle[Destinations.AddCommentCollectorArg])

    private val _form = MutableStateFlow(AddCommentFormState())
    val form: StateFlow<AddCommentFormState> = _form.asStateFlow()

    private val _uiState = MutableStateFlow<AddCommentUiState>(AddCommentUiState.Idle)
    val uiState: StateFlow<AddCommentUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddCommentEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AddCommentEvent> = _events.asSharedFlow()

    fun onDescriptionChange(value: String) {
        _form.update { it.copy(description = value, descriptionError = null) }
    }

    fun onRatingChange(value: Int) {
        _form.update { it.copy(rating = value.coerceIn(0, 5)) }
    }

    fun submit() {
        val current = _form.value
        val descriptionError = if (current.description.isBlank()) FormError.EmptyDescription else null

        if (descriptionError != null) {
            _form.update { it.copy(descriptionError = descriptionError) }
            return
        }

        _uiState.value = AddCommentUiState.Submitting
        viewModelScope.launch {
            when (val r = runCatchingDomain {
                addComment(
                    albumId = albumId,
                    description = current.description.trim(),
                    rating = current.rating,
                    collectorId = collectorId,
                )
            }) {
                is DomainResult.Ok -> _events.tryEmit(AddCommentEvent.Submitted)
                DomainResult.Network -> _uiState.value = AddCommentUiState.Error(DomainFailure.NETWORK)
                DomainResult.NotFound -> _uiState.value = AddCommentUiState.Error(DomainFailure.NOT_FOUND)
                DomainResult.Server -> _uiState.value = AddCommentUiState.Error(DomainFailure.SERVER)
            }
        }
    }

    fun resetError() {
        if (_uiState.value is AddCommentUiState.Error) {
            _uiState.value = AddCommentUiState.Idle
        }
    }
}
