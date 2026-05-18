package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AddCollectorAlbumRequest
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
import com.misw4203.vinilos.domain.model.PerformerKind
import com.misw4203.vinilos.di.IoDispatcher
import com.misw4203.vinilos.domain.repository.CollectorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class CollectorRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: CollectorDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CollectorRepository {

    override suspend fun getCollectors(): List<CollectorSummary> = withContext(ioDispatcher) {
        try {
            val summaries = api.getCollectors().map { it.toSummary() }
            dao.replaceCollectors(summaries.map { CollectorEntity.fromDomain(it) })
            summaries
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int) = withContext(ioDispatcher) {
        api.addMusicianToCollector(collectorId, musicianId)
        try {
            val cached = dao.getDetailById(collectorId)
            val musicianIdLong = musicianId.toLong()
            if (cached != null && cached.favoritePerformers.none { it.id == musicianIdLong }) {
                val musicianDto = api.getMusicianDetail(musicianId)
                val newFavorite = Performer(
                    id = musicianDto.id.toLong(),
                    name = musicianDto.name,
                    imageUrl = musicianDto.image,
                    kind = PerformerKind.MUSICIAN,
                )
                val updated = cached.copy(favoritePerformers = cached.favoritePerformers + newFavorite)
                dao.upsertDetail(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
        Unit
    }

    override suspend fun addFavoriteBand(collectorId: Int, bandId: Int) = withContext(ioDispatcher) {
        api.addBandToCollector(collectorId, bandId)
        try {
            val cached = dao.getDetailById(collectorId)
            val bandIdLong = bandId.toLong()
            if (cached != null && cached.favoritePerformers.none { it.id == bandIdLong }) {
                val bandDto = api.getBandDetail(bandId)
                val newFavorite = Performer(
                    id = bandDto.id.toLong(),
                    name = bandDto.name.orEmpty(),
                    imageUrl = bandDto.image.orEmpty(),
                    kind = PerformerKind.BAND,
                )
                val updated = cached.copy(favoritePerformers = cached.favoritePerformers + newFavorite)
                dao.upsertDetail(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
        Unit
    }

    override suspend fun getCollectorDetail(id: Int): CollectorDetail = withContext(ioDispatcher) {
        try {
            val dto = api.getCollectorDetail(id)
            val enrichedAlbums = coroutineScope {
                // GET /collectors/{id}/albums always returns album objects with the real album ID,
                // so we use it as the authoritative source when album is null in the detail DTO.
                val albumIdLookupAsync = async {
                    runCatching { api.getCollectorAlbums(id) }
                        .getOrElse { emptyList() }
                        .mapNotNull { ca -> ca.album?.id?.let { ca.id to it } }
                        .toMap()
                }
                val albumIdByAssocId = albumIdLookupAsync.await()
                dto.collectorAlbums.map { collAlbumDto ->
                    async {
                        val albumId = collAlbumDto.album?.id
                            ?: albumIdByAssocId[collAlbumDto.id]
                            ?: return@async collAlbumDto
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
        // `creationDate` is required for bands and absent for musicians, so it
        // is a reliable discriminator; `birthDate` is only a positive hint.
        kind = when {
            creationDate != null -> PerformerKind.BAND
            birthDate != null -> PerformerKind.MUSICIAN
            else -> PerformerKind.UNKNOWN
        },
    )

    override suspend fun addAlbumToCollector(
        collectorId: Int,
        albumId: Int,
        price: Double,
        status: String,
    ) = withContext(ioDispatcher) {
        api.addAlbumToCollector(collectorId, albumId, AddCollectorAlbumRequest(price, status))
        // La caché se reconciliará en el siguiente getCollectorDetail (sin invalidación explícita
        // porque CollectorDao no expone deleteDetail — misma estrategia que BandRepositoryImpl).
        Unit
    }

    override suspend fun removeFavoriteMusician(collectorId: Int, musicianId: Int) =
        withContext(ioDispatcher) {
            api.removeMusicianFromCollector(collectorId, musicianId)
            removeFavoriteFromCache(collectorId, musicianId.toLong())
        }

    override suspend fun removeFavoriteBand(collectorId: Int, bandId: Int) =
        withContext(ioDispatcher) {
            api.removeBandFromCollector(collectorId, bandId)
            removeFavoriteFromCache(collectorId, bandId.toLong())
        }

    override suspend fun removeAlbumFromCollector(collectorId: Int, albumId: Int) =
        withContext(ioDispatcher) {
            api.removeAlbumFromCollector(collectorId, albumId)
            try {
                val cached = dao.getDetailById(collectorId)
                if (cached != null) {
                    val updated = cached.copy(
                        collectorAlbums = cached.collectorAlbums.filterNot {
                            it.album?.id == albumId.toLong()
                        },
                    )
                    dao.upsertDetail(updated)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // best-effort; the next getCollectorDetail reconciles the cache
            }
            Unit
        }

    private suspend fun removeFavoriteFromCache(collectorId: Int, performerId: Long) {
        try {
            val cached = dao.getDetailById(collectorId)
            if (cached != null) {
                val updated = cached.copy(
                    favoritePerformers = cached.favoritePerformers.filterNot {
                        it.id == performerId
                    },
                )
                dao.upsertDetail(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
    }

    private fun CollectorCommentDto.toDomain() = CollectorComment(
        id = id,
        description = description.orEmpty(),
        rating = rating ?: 0,
        albumName = album?.name.orEmpty(),
    )
}
