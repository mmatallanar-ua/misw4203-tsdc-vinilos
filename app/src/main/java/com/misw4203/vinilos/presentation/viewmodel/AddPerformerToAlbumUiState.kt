package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary

data class AddPerformerToAlbumFormState(
    val query: String = "",
    val albumName: String = "",
    val selectedTab: PerformerType = PerformerType.MUSICIAN,
    val allMusicians: List<MusicianSummary> = emptyList(),
    val allBands: List<BandSummary> = emptyList(),
    val currentMusicianIds: Set<Int> = emptySet(),
    val currentBandIds: Set<Int> = emptySet(),
    val filteredMusicians: List<MusicianSummary> = emptyList(),
    val filteredBands: List<BandSummary> = emptyList(),
)

sealed interface AddPerformerToAlbumUiState {
    data object Loading : AddPerformerToAlbumUiState
    data object Ready : AddPerformerToAlbumUiState
    data class Adding(val type: PerformerType, val performerId: Int) : AddPerformerToAlbumUiState
    data class Error(
        val isNetworkError: Boolean,
        val type: PerformerType?,
        val performerId: Int?,
    ) : AddPerformerToAlbumUiState
}

sealed interface AddPerformerToAlbumEvent {
    data class AddedSuccessfully(val performerName: String) : AddPerformerToAlbumEvent
    data class AddFailed(val isNetworkError: Boolean) : AddPerformerToAlbumEvent
}
