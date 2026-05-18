package com.misw4203.vinilos.presentation.viewmodel

sealed interface AddMusiciansUiState {
    data object Loading : AddMusiciansUiState
    data object Ready : AddMusiciansUiState
    data class Adding(val musicianId: Int) : AddMusiciansUiState
    data class Error(val isNetworkError: Boolean, val musicianId: Int?) : AddMusiciansUiState
}

sealed interface AddMusiciansEvent {
    data class AddedSuccessfully(val musicianName: String) : AddMusiciansEvent
    data class AddFailed(val isNetworkError: Boolean) : AddMusiciansEvent
}
