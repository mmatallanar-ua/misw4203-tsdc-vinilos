package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.MusicianSummary

sealed interface MusicianListUiState {
    data object Loading : MusicianListUiState
    data class Success(val musicians: List<MusicianSummary>) : MusicianListUiState
    data object Empty : MusicianListUiState
    data class Error(val isNetworkError: Boolean) : MusicianListUiState
}
