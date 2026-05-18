package com.misw4203.vinilos.domain.model

data class Band(
    val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val creationDate: String,
    val members: List<MusicianSummary>,
    val albums: List<Album>,
    val prizes: List<MusicianPrize> = emptyList(),
)
