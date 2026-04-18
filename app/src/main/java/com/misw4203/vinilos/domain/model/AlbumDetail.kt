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
    val performers: List<Performer>,
    val comments: List<Comment>,
)

data class Track(
    val id: Long,
    val name: String,
    val duration: String,
)

data class Performer(
    val id: Long,
    val name: String,
    val imageUrl: String,
)

data class Comment(
    val id: Long,
    val description: String,
    val rating: Int,
)
