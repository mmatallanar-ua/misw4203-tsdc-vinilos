package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CollectorDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("telephone") val telephone: String,
    @SerializedName("email") val email: String,
    @SerializedName("description") val description: String? = null,
    @SerializedName("comments") val comments: List<Any> = emptyList(),
    @SerializedName("collectorAlbums") val collectorAlbums: List<Any> = emptyList(),
    @SerializedName("favoritePerformers") val favoritePerformers: List<Any> = emptyList(),
)
