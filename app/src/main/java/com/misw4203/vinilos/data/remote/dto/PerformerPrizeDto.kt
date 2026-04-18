package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PerformerPrizeDto(
    @SerializedName("id") val id: Int,
    @SerializedName("premiationDate") val premiationDate: String
)
