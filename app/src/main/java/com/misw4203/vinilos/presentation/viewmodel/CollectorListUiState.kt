package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.CollectorSummary

sealed interface CollectorListUiState {
    data object Loading : CollectorListUiState
    data class Success(val collectors: List<CollectorSummary>) : CollectorListUiState
    data object Empty : CollectorListUiState
    data class Error(val isNetworkError: Boolean) : CollectorListUiState
}
