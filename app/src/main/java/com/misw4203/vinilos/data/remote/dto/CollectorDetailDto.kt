package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CollectorDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("telephone") val telephone: String,
    @SerializedName("email") val email: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("comments") val comments: List<CollectorCommentDto> = emptyList(),
    @SerializedName("collectorAlbums") val collectorAlbums: List<CollectorAlbumDto> = emptyList(),
    @SerializedName("favoritePerformers") val favoritePerformers: List<PerformerDto> = emptyList(),
)

data class CollectorCommentDto(
    @SerializedName("id") val id: Long = 0L,
    @SerializedName("description") val description: String? = null,
    @SerializedName("rating") val rating: Int? = null,
    @SerializedName("album") val album: AlbumDto? = null,
)

data class CollectorAlbumDto(
    @SerializedName("id") val id: Int,
    @SerializedName("price") val price: Double,
    @SerializedName("status") val status: String,
    @SerializedName("album") val album: AlbumDto? = null,
)
