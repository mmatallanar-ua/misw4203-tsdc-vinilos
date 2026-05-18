package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.Prize

data class AddPrizeToBandFormState(
    val query: String = "",
    val allPrizes: List<Prize> = emptyList(),
    val filteredPrizes: List<Prize> = emptyList(),
    val selectedPrize: Prize? = null,
    val premiationDate: String? = null,
    val dateError: DateValidationError? = null,
    val currentPrizes: List<MusicianPrize> = emptyList(),
    val bandName: String = "",
    val bandImage: String = "",
)

val AddPrizeToBandFormState.isFormReady: Boolean
    get() = selectedPrize != null && premiationDate != null && dateError == null

sealed interface AddPrizeToBandUiState {
    data object Loading : AddPrizeToBandUiState
    data object Ready : AddPrizeToBandUiState
    data class Adding(val prizeId: Int) : AddPrizeToBandUiState
    data class Error(val isNetworkError: Boolean, val prizeId: Int? = null) : AddPrizeToBandUiState
}

sealed interface AddPrizeToBandEvent {
    data class AddedSuccessfully(val prizeName: String) : AddPrizeToBandEvent
    data class AddFailed(val isNetworkError: Boolean) : AddPrizeToBandEvent
}
