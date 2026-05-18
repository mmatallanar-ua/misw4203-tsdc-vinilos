package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.BandDao
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.BandDetailDto
import com.misw4203.vinilos.data.remote.dto.AddPrizeToMusicianRequest
import com.misw4203.vinilos.data.remote.dto.BandDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDetailDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.di.IoDispatcher
import com.misw4203.vinilos.domain.repository.BandRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class BandRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: BandDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BandRepository {

    override suspend fun getBands(): List<BandSummary> = withContext(ioDispatcher) {
        try {
            val summaries = api.getBands().map { it.toSummary() }
            dao.replaceBands(summaries.map { BandListEntity.fromDomain(it) })
            summaries
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun getBandDetail(id: Int): Band = withContext(ioDispatcher) {
        try {
            coroutineScope {
                val bandAsync = async { api.getBandDetail(id) }
                val allAssociationsAsync = async { api.getPerformerPrizes() }
                val dto = bandAsync.await()
                val associationMap: Map<Int, PerformerPrizeDetailDto> =
                    allAssociationsAsync.await().associateBy { it.id }
                val prizes = dto.performerPrizes.orEmpty().mapNotNull { pp ->
                    val assoc = associationMap[pp.id] ?: return@mapNotNull null
                    val prize = assoc.prize ?: return@mapNotNull null
                    MusicianPrize(
                        id = prize.id,
                        name = prize.name,
                        organization = prize.organization,
                        description = prize.description,
                        premiationDate = pp.premiationDate,
                    )
                }
                val band = dto.toDomain(prizes)
                dao.upsertDetail(BandDetailEntity.fromDomain(band))
                band
            }
        } catch (e: IOException) {
            dao.getDetailById(id)?.toDomain() ?: throw e
        }
    }

    override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = withContext(ioDispatcher) {
        api.addMusicianToBand(bandId, musicianId)
        // Write-through best-effort: si el detalle esta cacheado, intentamos
        // refrescar members. Un fallo aqui no debe propagarse: la cache se
        // reconciliara con el servidor en el proximo getBandDetail.
        try {
            val cached = dao.getDetailById(bandId)
            if (cached != null) {
                val musicianDto = api.getMusicianDetail(musicianId)
                val newMember = MusicianSummary(
                    id = musicianDto.id,
                    name = musicianDto.name,
                    image = musicianDto.image,
                    birthDate = musicianDto.birthDate,
                )
                val updated = cached.copy(members = cached.members + newMember)
                dao.upsertDetail(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
        Unit
    }

    override suspend fun addAlbumToBand(bandId: Int, albumId: Long) = withContext(ioDispatcher) {
        api.addAlbumToBand(bandId, albumId)
        // Write-through best-effort: ver nota en addMusicianToBand.
        try {
            val cached = dao.getDetailById(bandId)
            if (cached != null && cached.albums.none { it.id == albumId }) {
                val albumDto = api.getAlbum(albumId)
                val updated = cached.copy(albums = cached.albums + albumDto.toDomain())
                dao.upsertDetail(updated)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // best-effort
        }
        Unit
    }

    override suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) =
        withContext(ioDispatcher) {
            api.addPrizeToBand(prizeId, bandId, AddPrizeToMusicianRequest(premiationDate))
            Unit
        }

    private fun BandDto.toSummary() = BandSummary(
        id = id,
        name = name.orEmpty(),
        image = image.orEmpty(),
    )

    private fun BandDetailDto.toDomain(prizes: List<MusicianPrize>) = Band(
        id = id,
        name = name.orEmpty(),
        image = image.orEmpty(),
        description = description.orEmpty(),
        creationDate = creationDate.orEmpty(),
        members = musicians.orEmpty().map {
            MusicianSummary(
                id = it.id,
                name = it.name,
                image = it.image,
                birthDate = it.birthDate,
            )
        },
        albums = albums.orEmpty().map { it.toDomain() },
        prizes = prizes,
    )

    private fun AlbumDto.toDomain() = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )
}
