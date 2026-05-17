package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CreatePrizeRequest
import com.misw4203.vinilos.data.remote.dto.toDomain
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.PrizeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PrizeRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
) : PrizeRepository {

    override suspend fun getPrizes(): List<Prize> = withContext(Dispatchers.IO) {
        api.getPrizes().map { it.toDomain() }
    }

    override suspend fun createPrize(name: String, description: String, organization: String): Prize =
        withContext(Dispatchers.IO) {
            api.createPrize(CreatePrizeRequest(name, description, organization)).toDomain()
        }
}
