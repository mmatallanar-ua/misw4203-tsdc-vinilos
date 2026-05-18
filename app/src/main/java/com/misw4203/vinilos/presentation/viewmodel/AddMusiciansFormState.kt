package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.MusicianSummary

data class AddMusiciansFormState(
    val query: String = "",
    val allMusicians: List<MusicianSummary> = emptyList(),
    val currentMemberIds: Set<Int> = emptySet(),
    val filteredAvailable: List<MusicianSummary> = emptyList(),
)
