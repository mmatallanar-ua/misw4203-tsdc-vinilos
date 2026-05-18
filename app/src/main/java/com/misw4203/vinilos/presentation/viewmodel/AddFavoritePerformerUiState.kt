package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianSummary

enum class PerformerType { MUSICIAN, BAND }

data class AddFavoritePerformerFormState(
    val query: String = "",
    val collectorName: String = "",
    val selectedTab: PerformerType = PerformerType.MUSICIAN,
    val allMusicians: List<MusicianSummary> = emptyList(),
    val allBands: List<BandSummary> = emptyList(),
    val favoriteMusicianIds: Set<Int> = emptySet(),
    val favoriteBandIds: Set<Int> = emptySet(),
    val filteredMusicians: List<MusicianSummary> = emptyList(),
    val filteredBands: List<BandSummary> = emptyList(),
)

sealed interface AddFavoritePerformerUiState {
    data object Loading : AddFavoritePerformerUiState
    data object Ready : AddFavoritePerformerUiState
    data class Adding(val type: PerformerType, val performerId: Int) : AddFavoritePerformerUiState
    data class Error(
        val isNetworkError: Boolean,
        val type: PerformerType?,
        val performerId: Int?,
    ) : AddFavoritePerformerUiState
}

sealed interface AddFavoritePerformerEvent {
    data class AddedSuccessfully(val performerName: String) : AddFavoritePerformerEvent
    data class AddFailed(val isNetworkError: Boolean) : AddFavoritePerformerEvent
}
