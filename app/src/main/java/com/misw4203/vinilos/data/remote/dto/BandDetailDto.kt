package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BandDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("creationDate") val creationDate: String?,
    @SerializedName("musicians") val musicians: List<MusicianDetailDto>?,
    @SerializedName("albums") val albums: List<AlbumDto>?,
    @SerializedName("performerPrizes") val performerPrizes: List<PerformerPrizeDto>? = null,
)
