package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class CollectorRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: CollectorDao,
) : CollectorRepository {

    override suspend fun getCollectors(): List<CollectorSummary> = withContext(Dispatchers.IO) {
        try {
            val summaries = api.getCollectors().map { it.toSummary() }
            dao.replaceCollectors(summaries.map { CollectorEntity.fromDomain(it) })
            summaries
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    private fun CollectorDto.toSummary() = CollectorSummary(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
    )
}
