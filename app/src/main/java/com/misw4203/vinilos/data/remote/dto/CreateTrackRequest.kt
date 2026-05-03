package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateTrackRequest(
    @SerializedName("name")     val name: String,
    @SerializedName("duration") val duration: String,
)
