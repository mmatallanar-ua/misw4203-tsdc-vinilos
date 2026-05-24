package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CreatePrizeRequest
import com.misw4203.vinilos.data.remote.dto.toDomain
import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.di.IoDispatcher
import com.misw4203.vinilos.domain.repository.PrizeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PrizeRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PrizeRepository {

    override suspend fun getPrizes(): List<Prize> = withContext(ioDispatcher) {
        api.getPrizes().map { it.toDomain() }
    }

    override suspend fun createPrize(name: String, description: String, organization: String): Prize =
        withContext(ioDispatcher) {
            api.createPrize(CreatePrizeRequest(name, description, organization)).toDomain()
        }
}
