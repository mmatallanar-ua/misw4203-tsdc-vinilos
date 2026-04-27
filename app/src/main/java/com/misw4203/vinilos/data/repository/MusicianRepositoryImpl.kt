package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class MusicianRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: MusicianDao,
) : MusicianRepository {

    override suspend fun getMusicians(): List<MusicianSummary> = withContext(Dispatchers.IO) {
        try {
            val summaries = api.getMusicians().map { it.toSummary() }
            dao.replaceMusicians(summaries.map { MusicianListEntity.fromDomain(it) })
            summaries
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun getMusicianDetail(id: Int): Musician = withContext(Dispatchers.IO) {
        try {
            val dto = api.getMusicianDetail(id)
            val prizes = coroutineScope {
                dto.performerPrizes.map { pp ->
                    async {
                        val prizeDto = api.getPrizeDetail(pp.id)
                        MusicianPrize(
                            id = prizeDto.id,
                            name = prizeDto.name,
                            organization = prizeDto.organization,
                            description = prizeDto.description,
                            premiationDate = pp.premiationDate,
                        )
                    }
                }.awaitAll()
            }
            val musician = dto.toDomain(prizes)
            dao.upsertDetail(MusicianDetailEntity.fromDomain(musician))
            musician
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

    private fun AlbumDto.toDomain() = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )
}
