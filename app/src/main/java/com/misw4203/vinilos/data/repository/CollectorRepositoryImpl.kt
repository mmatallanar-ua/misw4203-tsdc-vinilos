package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorAlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorCommentDto
import com.misw4203.vinilos.data.remote.dto.CollectorDetailDto
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.data.remote.dto.PerformerDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.repository.CollectorRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    override suspend fun getCollectorDetail(id: Int): CollectorDetail = withContext(Dispatchers.IO) {
        try {
            val dto = api.getCollectorDetail(id)
            // Fetch full album data in parallel to get name + artistName
            val enrichedAlbums = coroutineScope {
                dto.collectorAlbums.map { collAlbumDto ->
                    async {
                        // The /collectors/{id} endpoint doesn't nest the album object, so we
                        // fall back to fetching /albums/{collectorAlbum.id} — the backend's
                        // seed data uses matching IDs for the association and the album itself.
                        val albumId = collAlbumDto.album?.id ?: collAlbumDto.id.toLong()
                        val fullAlbum = runCatching { api.getAlbum(albumId) }
                            .getOrElse { collAlbumDto.album }
                        collAlbumDto.copy(album = fullAlbum)
                    }
                }.awaitAll()
            }
            val detail = dto.copy(collectorAlbums = enrichedAlbums).toDomain()
            dao.upsertDetail(CollectorDetailEntity.fromDomain(detail))
            detail
        } catch (e: IOException) {
            dao.getDetailById(id)?.toDomain() ?: throw e
        }
    }

    private fun CollectorDto.toSummary() = CollectorSummary(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
    )

    private fun CollectorDetailDto.toDomain() = CollectorDetail(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
        description = description.orEmpty(),
        collectorAlbums = collectorAlbums.map { it.toDomain() },
        favoritePerformers = favoritePerformers.map { it.toDomain() },
        comments = comments.map { it.toDomain() },
    )

    private fun CollectorAlbumDto.toDomain() = CollectorAlbum(
        id = id,
        price = price,
        status = status,
        album = album?.toDomain(),
    )

    private fun AlbumDto.toDomain() = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )

    private fun PerformerDto.toDomain() = Performer(
        id = id,
        name = name.orEmpty(),
        imageUrl = image.orEmpty(),
    )

    private fun CollectorCommentDto.toDomain() = CollectorComment(
        id = id,
        description = description.orEmpty(),
        rating = rating ?: 0,
        albumName = album?.name.orEmpty(),
    )
}
