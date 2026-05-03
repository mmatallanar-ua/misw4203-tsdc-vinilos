package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateCommentRequest(
    @SerializedName("description") val description: String,
    @SerializedName("rating") val rating: Int,
    @SerializedName("collector") val collector: CollectorRef,
)

data class CollectorRef(
    @SerializedName("id") val id: Int,
)
