package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PerformerPrizeDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("premiationDate") val premiationDate: String,
    @SerializedName("prize") val prize: PrizeInAssociationDto?,
)

data class PrizeInAssociationDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("organization") val organization: String,
)
