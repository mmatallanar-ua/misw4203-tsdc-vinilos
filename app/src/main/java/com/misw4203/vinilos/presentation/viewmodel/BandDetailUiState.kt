package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Band

sealed interface BandDetailUiState {
    data object Loading : BandDetailUiState
    data class Success(val band: Band) : BandDetailUiState
    data object NotFound : BandDetailUiState
    data class Error(val isNetworkError: Boolean) : BandDetailUiState
}
