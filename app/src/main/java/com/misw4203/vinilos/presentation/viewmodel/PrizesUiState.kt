package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Prize

sealed interface PrizesUiState {
    data object Loading : PrizesUiState
    data class Success(val prizes: List<Prize>) : PrizesUiState
    data object Empty : PrizesUiState
    data class Error(val isNetworkError: Boolean) : PrizesUiState
}
