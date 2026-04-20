package com.misw4203.vinilos.domain.model

data class Musician(
    val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val birthDate: String,
    val albums: List<Album>,
    val prizes: List<MusicianPrize>
)
