package com.misw4203.vinilos.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.misw4203.vinilos.domain.model.Prize

data class PrizeDetailDto(
    @SerializedName("id") val id: Int,
    @SerializedName("organization") val organization: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("performerPrizes") val performerPrizes: List<PerformerPrizeDto> = emptyList(),
)

fun PrizeDetailDto.toDomain() = Prize(id = id, name = name, description = description, organization = organization)
