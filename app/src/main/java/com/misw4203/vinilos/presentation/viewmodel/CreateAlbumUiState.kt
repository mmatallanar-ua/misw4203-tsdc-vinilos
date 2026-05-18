package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.presentation.common.DomainFailure

sealed interface CreateAlbumUiState {
    data object Idle : CreateAlbumUiState
    data object Submitting : CreateAlbumUiState
    data class Error(val category: DomainFailure) : CreateAlbumUiState
}

sealed interface CreateAlbumEvent {
    data object Submitted : CreateAlbumEvent
}
