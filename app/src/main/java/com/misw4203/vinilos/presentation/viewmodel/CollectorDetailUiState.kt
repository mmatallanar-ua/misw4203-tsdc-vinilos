package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Collector

sealed interface CollectorDetailUiState {
    data object Loading : CollectorDetailUiState
    data class Success(val collector: Collector) : CollectorDetailUiState
    data object NotFound : CollectorDetailUiState
    data class Error(val isNetworkError: Boolean) : CollectorDetailUiState
}
