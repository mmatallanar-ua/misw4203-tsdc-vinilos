package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.BandSummary

sealed interface BandListUiState {
    data object Loading : BandListUiState
    data class Success(val bands: List<BandSummary>) : BandListUiState
    data object Empty : BandListUiState
    data class Error(val isNetworkError: Boolean) : BandListUiState
}
