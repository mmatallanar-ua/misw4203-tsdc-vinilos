package com.misw4203.vinilos.presentation.viewmodel

sealed interface CreateAlbumUiState {
    data object Idle : CreateAlbumUiState
    data object Submitting : CreateAlbumUiState
    data object Success : CreateAlbumUiState
    data class Error(val message: String) : CreateAlbumUiState
}
