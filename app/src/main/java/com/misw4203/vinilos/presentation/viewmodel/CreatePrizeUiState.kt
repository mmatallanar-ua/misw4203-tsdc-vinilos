package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.presentation.common.DomainFailure

sealed interface CreatePrizeUiState {
    data object LoadingPrizes : CreatePrizeUiState
    data class Ready(val existingPrizes: List<Prize>) : CreatePrizeUiState
    data object Submitting : CreatePrizeUiState
    data class Error(val category: DomainFailure) : CreatePrizeUiState
}

sealed interface CreatePrizeEvent {
    data object Submitted : CreatePrizeEvent
    data class ValidationFailed(val reason: PrizeValidation) : CreatePrizeEvent
}

enum class PrizeValidation { BLANK_FIELDS, DUPLICATE }
