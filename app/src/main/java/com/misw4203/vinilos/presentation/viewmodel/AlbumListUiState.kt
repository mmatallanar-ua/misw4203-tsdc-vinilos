package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Album

sealed interface AlbumListUiState {
    data object Loading : AlbumListUiState
    data class Success(val albums: List<Album>) : AlbumListUiState
    data object Empty : AlbumListUiState
    data class Error(val isNetworkError: Boolean) : AlbumListUiState
}
