package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.presentation.common.DomainFailure

sealed interface AddTrackUiState {
    data object Idle : AddTrackUiState
    data object Submitting : AddTrackUiState
    data class Error(val category: DomainFailure) : AddTrackUiState
}

sealed interface AddTrackEvent {
    data object Submitted : AddTrackEvent
}
