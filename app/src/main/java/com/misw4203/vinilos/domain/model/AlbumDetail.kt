package com.misw4203.vinilos.domain.model

data class AlbumDetail(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val releaseDate: String,
    val genre: String,
    val recordLabel: String,
    val description: String,
    val tracks: List<Track>,
)

data class Track(
    val id: Long,
    val name: String,
    val duration: String,
)
