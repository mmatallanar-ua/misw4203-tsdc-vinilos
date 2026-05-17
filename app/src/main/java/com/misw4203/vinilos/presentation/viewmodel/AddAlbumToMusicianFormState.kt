package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Album

data class AddAlbumToMusicianFormState(
    val query: String = "",
    val allAlbums: List<Album> = emptyList(),
    val currentAlbumIds: Set<Long> = emptySet(),
    val filteredAvailable: List<Album> = emptyList(),
)
