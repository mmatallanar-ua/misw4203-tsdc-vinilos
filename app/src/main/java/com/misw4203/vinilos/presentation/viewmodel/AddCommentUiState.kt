package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Comment

sealed interface AddCommentUiState {
    data object Idle : AddCommentUiState
    data object Loading : AddCommentUiState
    data class Success(val comment: Comment) : AddCommentUiState
    data class Error(val isNetworkError: Boolean) : AddCommentUiState
}

data class AddCommentFormState(
    val description: String = "",
    val rating: Int = 0,
    val descriptionError: FormError? = null,
    val ratingError: FormError? = null,
)

enum class FormError {
    EmptyDescription,
    InvalidRating,
}
