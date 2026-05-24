package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.presentation.common.DomainFailure

sealed interface AddCommentUiState {
    data object Idle : AddCommentUiState
    data object Submitting : AddCommentUiState
    data class Error(val category: DomainFailure) : AddCommentUiState
}

sealed interface AddCommentEvent {
    data object Submitted : AddCommentEvent
}

data class AddCommentFormState(
    val description: String = "",
    val rating: Int = 0,
    val descriptionError: FormError? = null,
)

enum class FormError {
    EmptyDescription,
}
