package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Prize

sealed interface CreatePrizeUiState {
    data object LoadingPrizes : CreatePrizeUiState
    data class Ready(val existingPrizes: List<Prize>) : CreatePrizeUiState
    data object Submitting : CreatePrizeUiState
    data object Success : CreatePrizeUiState
    data class Error(val message: String) : CreatePrizeUiState
}
