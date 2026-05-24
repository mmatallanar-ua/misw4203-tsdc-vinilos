package com.misw4203.vinilos.presentation.viewmodel

sealed interface AddAlbumToBandUiState {
    data object Loading : AddAlbumToBandUiState
    data object Ready : AddAlbumToBandUiState
    data class Adding(val albumId: Long) : AddAlbumToBandUiState
    data class Error(val isNetworkError: Boolean, val albumId: Long?) : AddAlbumToBandUiState
}

sealed interface AddAlbumToBandEvent {
    data class AddedSuccessfully(val albumName: String) : AddAlbumToBandEvent
    data class AddFailed(val isNetworkError: Boolean) : AddAlbumToBandEvent
}
