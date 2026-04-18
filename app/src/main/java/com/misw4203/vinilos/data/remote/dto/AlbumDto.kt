package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AlbumDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("recordLabel") val recordLabel: String?,
    @SerializedName("performers") val performers: List<PerformerDto>?,
    @SerializedName("tracks") val tracks: List<TrackDto>?,
    @SerializedName("comments") val comments: List<CommentDto>?,
)

data class PerformerDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("birthDate") val birthDate: String?,
    @SerializedName("creationDate") val creationDate: String?,
)

data class TrackDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String?,
    @SerializedName("duration") val duration: String?,
)

data class CommentDto(
    @SerializedName("id") val id: Long,
    @SerializedName("description") val description: String?,
    @SerializedName("rating") val rating: Int?,
)
