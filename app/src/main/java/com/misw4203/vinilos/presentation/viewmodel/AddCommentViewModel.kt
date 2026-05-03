package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misw4203.vinilos.domain.usecase.AddCommentUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
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

    fun onDescriptionChange(value: String) {
        _form.update { it.copy(description = value, descriptionError = null) }
    }

    fun onRatingChange(value: Int) {
        _form.update { it.copy(rating = value.coerceIn(0, 5), ratingError = null) }
    }

    fun submit() {
        val current = _form.value
        val descriptionError = if (current.description.isBlank()) FormError.EmptyDescription else null
        val ratingError = if (current.rating < 1) FormError.InvalidRating else null

        if (descriptionError != null || ratingError != null) {
            _form.update {
                it.copy(descriptionError = descriptionError, ratingError = ratingError)
            }
            return
        }

        _uiState.value = AddCommentUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val comment = addComment(
                    albumId = albumId,
                    description = current.description.trim(),
                    rating = current.rating,
                    collectorId = collectorId,
                )
                AddCommentUiState.Success(comment)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                AddCommentUiState.Error(isNetworkError = true)
            } catch (e: Exception) {
                AddCommentUiState.Error(isNetworkError = false)
            }
        }
    }

    fun resetError() {
        if (_uiState.value is AddCommentUiState.Error) {
            _uiState.value = AddCommentUiState.Idle
        }
    }
}
