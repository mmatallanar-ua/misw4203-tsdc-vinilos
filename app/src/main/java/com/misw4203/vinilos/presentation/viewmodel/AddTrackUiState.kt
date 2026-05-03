package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Track

sealed interface AddTrackUiState {
    data object Idle : AddTrackUiState
    data object Loading : AddTrackUiState
    data class Success(val track: Track) : AddTrackUiState
    data class Error(val message: String, val isNetworkError: Boolean) : AddTrackUiState
}
