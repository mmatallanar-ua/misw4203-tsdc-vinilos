package com.misw4203.vinilos.presentation.viewmodel

sealed interface AddAlbumToMusicianUiState {
    data object Loading : AddAlbumToMusicianUiState
    data object Ready : AddAlbumToMusicianUiState
    data class Adding(val albumId: Int) : AddAlbumToMusicianUiState
    data class Error(val isNetworkError: Boolean, val albumId: Int?) : AddAlbumToMusicianUiState
}

sealed interface AddAlbumToMusicianEvent {
    data class AddedSuccessfully(val albumName: String) : AddAlbumToMusicianEvent
    data class AddFailed(val isNetworkError: Boolean) : AddAlbumToMusicianEvent
}
