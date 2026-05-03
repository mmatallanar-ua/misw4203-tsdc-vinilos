package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorRef
import com.misw4203.vinilos.data.remote.dto.CreateCommentRequest
import com.misw4203.vinilos.data.remote.dto.CreateAlbumRequestDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val dao: AlbumDao,
) : AlbumRepository {

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        try {
            val albums = api.getAlbums().map { it.toAlbum() }
            dao.replaceAlbums(albums.map { AlbumEntity.fromDomain(it) })
            albums
        } catch (e: IOException) {
            val cached = dao.getAll()
            if (cached.isNotEmpty()) cached.map { it.toDomain() } else throw e
        }
    }

    override suspend fun getAlbumById(id: Long): AlbumDetail = withContext(Dispatchers.IO) {
        try {
            val detail = api.getAlbum(id).toAlbumDetail()
            dao.upsertDetail(AlbumDetailEntity.fromDomain(detail))
            detail
        } catch (e: IOException) {
            dao.getDetailById(id)?.toDomain() ?: throw e
        }
    }

    override suspend fun addComment(
        albumId: Long,
        description: String,
        rating: Int,
        collectorId: Int,
    ): Comment = withContext(Dispatchers.IO) {
        val response = api.addComment(
            albumId = albumId,
            request = CreateCommentRequest(
                description = description,
                rating = rating,
                collector = CollectorRef(id = collectorId),
            ),
        )
        Comment(
            id = response.id,
            description = response.description.orEmpty(),
            rating = response.rating ?: rating,
        )
    }

    override suspend fun createAlbum(input: CreateAlbumInput): Album = withContext(Dispatchers.IO) {
        val dto = api.createAlbum(
            CreateAlbumRequestDto(
                name = input.name,
                cover = input.cover,
                releaseDate = input.releaseDate,
                description = input.description,
                genre = input.genre,
                recordLabel = input.recordLabel,
            )
        )
        dto.toAlbum()
    }

    private fun AlbumDto.toAlbum(): Album = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )

    private fun AlbumDto.toAlbumDetail(): AlbumDetail = AlbumDetail(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseDate = releaseDate.orEmpty(),
        genre = genre.orEmpty(),
        recordLabel = recordLabel.orEmpty(),
        description = description.orEmpty(),
        tracks = tracks?.map { track ->
            Track(
                id = track.id,
                name = track.name.orEmpty(),
                duration = track.duration.orEmpty(),
            )
        }.orEmpty(),
        performers = performers?.map { p ->
            Performer(
                id = p.id,
                name = p.name.orEmpty(),
                imageUrl = p.image.orEmpty(),
            )
        }.orEmpty(),
        comments = comments?.map { c ->
            Comment(
                id = c.id,
                description = c.description.orEmpty(),
                rating = c.rating ?: 0,
            )
        }.orEmpty(),
    )
}
