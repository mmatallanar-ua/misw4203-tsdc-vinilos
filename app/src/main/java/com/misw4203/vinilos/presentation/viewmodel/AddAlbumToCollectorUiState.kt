package com.misw4203.vinilos.presentation.viewmodel

sealed interface AddAlbumToCollectorUiState {
    object Loading : AddAlbumToCollectorUiState
    object Ready : AddAlbumToCollectorUiState
    data class Adding(val albumId: Int) : AddAlbumToCollectorUiState
    data class Error(val isNetworkError: Boolean, val albumId: Int?) : AddAlbumToCollectorUiState
}

sealed interface AddAlbumToCollectorEvent {
    data class AddedSuccessfully(val albumName: String) : AddAlbumToCollectorEvent
    data class AddFailed(val isNetworkError: Boolean) : AddAlbumToCollectorEvent
}
