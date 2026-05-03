package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateAlbumRequestDto(
    @SerializedName("name") val name: String,
    @SerializedName("cover") val cover: String,
    @SerializedName("releaseDate") val releaseDate: String,
    @SerializedName("description") val description: String,
    @SerializedName("genre") val genre: String,
    @SerializedName("recordLabel") val recordLabel: String,
)
