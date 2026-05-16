package com.misw4203.vinilos.presentation.viewmodel

import com.misw4203.vinilos.domain.model.Album

data class AddAlbumToCollectorFormState(
    val query: String = "",
    val allAlbums: List<Album> = emptyList(),
    val currentAlbumIds: Set<Long> = emptySet(),
    val filteredAvailable: List<Album> = emptyList(),
    val selectedAlbum: Album? = null,
    val price: String = "",
    val status: String = "Active",
)

val AddAlbumToCollectorFormState.isPriceValid: Boolean
    get() = price.toDoubleOrNull()?.let { it > 0.0 } ?: false

val AddAlbumToCollectorFormState.isFormReady: Boolean
    get() = selectedAlbum != null && isPriceValid
