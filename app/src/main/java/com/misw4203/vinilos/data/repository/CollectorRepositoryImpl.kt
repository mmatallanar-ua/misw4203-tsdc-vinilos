package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CollectorAlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.FavoritePerformerDto
import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.FavoritePerformer
import com.misw4203.vinilos.domain.repository.CollectorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class CollectorRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val collectorDao: CollectorDao,
) : CollectorRepository {

    override suspend fun getCollectors(): List<CollectorSummary> = withContext(Dispatchers.IO) {
        try {
            val remote = api.getCollectors().map { it.toSummary() }
            collectorDao.replaceCollectors(remote.map(CollectorEntity::fromDomain))
            remote
        } catch (e: IOException) {
            val cached = collectorDao.getCollectors()
            if (cached.isEmpty()) throw e
            cached.map(CollectorEntity::toDomain)
        }
    }

    override suspend fun getCollectorById(id: Int): Collector = withContext(Dispatchers.IO) {
        try {
            val remote = api.getCollector(id).toDomain()
            collectorDao.upsertCollectorDetail(CollectorDetailEntity.fromDomain(remote))
            remote
        } catch (e: IOException) {
            collectorDao.getCollectorDetail(id)?.toDomain() ?: throw e
        }
    }

    private fun CollectorDto.toSummary() = CollectorSummary(
        id = id,
        name = name.orEmpty(),
        email = email.orEmpty(),
        telephone = telephone.orEmpty(),
    )

    private fun CollectorDto.toDomain() = Collector(
        id = id,
        name = name.orEmpty(),
        telephone = telephone.orEmpty(),
        email = email.orEmpty(),
        comments = comments?.map { it.toDomain() }.orEmpty(),
        favoritePerformers = favoritePerformers?.map { it.toDomain() }.orEmpty(),
        collectorAlbums = collectorAlbums?.map { it.toDomain() }.orEmpty(),
    )

    private fun CommentDto.toDomain() = Comment(
        id = id,
        description = description.orEmpty(),
        rating = rating ?: 0,
    )

    private fun FavoritePerformerDto.toDomain() = FavoritePerformer(
        id = id,
        name = name.orEmpty(),
        image = image.orEmpty(),
        description = description.orEmpty(),
    )

    private fun CollectorAlbumDto.toDomain() = CollectorAlbum(
        id = id,
        price = price ?: 0,
        status = status.orEmpty(),
    )
}
