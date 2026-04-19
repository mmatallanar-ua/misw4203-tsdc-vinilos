package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CollectorDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("telephone") val telephone: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("comments") val comments: List<CommentDto>?,
    @SerializedName("favoritePerformers") val favoritePerformers: List<FavoritePerformerDto>?,
    @SerializedName("collectorAlbums") val collectorAlbums: List<CollectorAlbumDto>?,
)

data class FavoritePerformerDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("birthDate") val birthDate: String?,
    @SerializedName("creationDate") val creationDate: String?,
)

data class CollectorAlbumDto(
    @SerializedName("id") val id: Long,
    @SerializedName("price") val price: Int?,
    @SerializedName("status") val status: String?,
)
