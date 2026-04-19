package com.misw4203.vinilos.domain.model

data class Collector(
    val id: Int,
    val name: String,
    val telephone: String,
    val email: String,
    val comments: List<Comment>,
    val favoritePerformers: List<FavoritePerformer>,
    val collectorAlbums: List<CollectorAlbum>,
)

data class FavoritePerformer(
    val id: Int,
    val name: String,
    val image: String,
    val description: String,
)

data class CollectorAlbum(
    val id: Long,
    val price: Int,
    val status: String,
)
