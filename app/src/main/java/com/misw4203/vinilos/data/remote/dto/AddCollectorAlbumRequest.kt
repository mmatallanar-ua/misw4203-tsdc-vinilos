package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AddCollectorAlbumRequest(
    @SerializedName("price") val price: Double,
    @SerializedName("status") val status: String,
)
