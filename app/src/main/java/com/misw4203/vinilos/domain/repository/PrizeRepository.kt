package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Prize

interface PrizeRepository {
    suspend fun getPrizes(): List<Prize>
    suspend fun createPrize(name: String, description: String, organization: String): Prize
}
