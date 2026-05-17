package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreatePrizeRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("organization") val organization: String,
)
