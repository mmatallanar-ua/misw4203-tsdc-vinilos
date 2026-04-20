package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.AlbumDetail

sealed interface AlbumDetailUiState {
    data object Loading : AlbumDetailUiState
    data class Success(val album: AlbumDetail) : AlbumDetailUiState
    data object NotFound : AlbumDetailUiState
    data class Error(val isNetworkError: Boolean) : AlbumDetailUiState
}
