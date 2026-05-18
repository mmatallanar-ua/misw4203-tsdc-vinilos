package com.misw4203.vinilos.presentation.viewmodel

sealed interface AddPrizeToMusicianUiState {
    data object Loading : AddPrizeToMusicianUiState
    data object Ready : AddPrizeToMusicianUiState
    data class Adding(val prizeId: Int) : AddPrizeToMusicianUiState
    data class Error(val isNetworkError: Boolean, val prizeId: Int? = null) : AddPrizeToMusicianUiState
}
