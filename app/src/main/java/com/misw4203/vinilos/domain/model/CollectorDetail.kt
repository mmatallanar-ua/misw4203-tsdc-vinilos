package com.misw4203.vinilos.domain.model

data class CollectorDetail(
    val id: Int,
    val name: String,
    val telephone: String,
    val email: String,
    val description: String,
    val collectorAlbums: List<CollectorAlbum>,
    val favoritePerformers: List<Performer>,
    val comments: List<CollectorComment>,
)

data class CollectorComment(
    val id: Long,
    val description: String,
    val rating: Int,
    val albumName: String,
)

data class CollectorAlbum(
    val id: Int,
    val price: Double,
    val status: String,
    val album: Album?,
)
