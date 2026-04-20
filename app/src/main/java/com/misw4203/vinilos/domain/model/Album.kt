package com.misw4203.vinilos.domain.model

data class Album(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val releaseYear: String,
    val genre: String,
)
