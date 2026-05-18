package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AddPrizeToMusicianRequest
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDetailDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.di.IoDispatcher
import com.misw4203.vinilos.domain.repository.MusicianRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class MusicianRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: MusicianDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MusicianRepository {

    override suspend fun getMusicians(): List<MusicianSummary> = withContext(ioDispatcher) {
        try {
            val summaries = api.getMusicians().map { it.toSummary() }
            dao.replaceMusicians(summaries.map { MusicianListEntity.fromDomain(it) })
            summaries
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun getMusicianDetail(id: Int): Musician = withContext(ioDispatcher) {
        try {
            coroutineScope {
                val musicianAsync = async { api.getMusicianDetail(id) }
                val allAssociationsAsync = async { api.getPerformerPrizes() }
                val dto = musicianAsync.await()
                val associationMap: Map<Int, PerformerPrizeDetailDto> =
                    allAssociationsAsync.await().associateBy { it.id }
                val prizes = dto.performerPrizes.mapNotNull { pp ->
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
                val musician = dto.toDomain(prizes)
                dao.upsertDetail(MusicianDetailEntity.fromDomain(musician))
                musician
            }
        } catch (e: IOException) {
            dao.getDetailById(id)?.toDomain() ?: throw e
        }
    }

    private fun MusicianDetailDto.toSummary() = MusicianSummary(
        id = id,
        name = name,
        image = image,
        birthDate = birthDate,
    )

    private fun MusicianDetailDto.toDomain(prizes: List<MusicianPrize>) = Musician(
        id = id,
        name = name,
        image = image,
        description = description,
        birthDate = birthDate,
        albums = albums.map { it.toDomain() },
        prizes = prizes,
    )

    override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = withContext(ioDispatcher) {
        api.addAlbumToMusician(musicianId, albumId)
        try {
            val cached = dao.getDetailById(musicianId)
            if (cached != null) {
                val newAlbum = api.getAlbum(albumId.toLong()).toDomain()
                dao.upsertDetail(cached.copy(albums = cached.albums + newAlbum))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) { /* best-effort */ }
        Unit
    }

    override suspend fun addPrizeToMusician(musicianId: Int, prizeId: Int, premiationDate: String) =
        withContext(ioDispatcher) {
            api.addPrizeToMusician(prizeId, musicianId, AddPrizeToMusicianRequest(premiationDate))
            Unit
        }

    private fun AlbumDto.toDomain() = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )
}
