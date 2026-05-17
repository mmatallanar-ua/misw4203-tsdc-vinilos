package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AddPrizeToMusicianRequest(
    @SerializedName("premiationDate") val premiationDate: String,
)
