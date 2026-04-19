package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
    private val albumDao: AlbumDao,
) : AlbumRepository {

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        try {
            val remote = api.getAlbums().map { it.toAlbum() }
            albumDao.replaceAlbums(remote.map(AlbumEntity::fromDomain))
            remote
        } catch (e: IOException) {
            val cached = albumDao.getAlbums()
            if (cached.isEmpty()) throw e
            cached.map(AlbumEntity::toDomain)
        }
    }

    override suspend fun getAlbumById(id: Long): AlbumDetail = withContext(Dispatchers.IO) {
        try {
            val remote = api.getAlbum(id).toAlbumDetail()
            albumDao.upsertAlbumDetail(AlbumDetailEntity.fromDomain(remote))
            remote
        } catch (e: IOException) {
            albumDao.getAlbumDetail(id)?.toDomain() ?: throw e
        }
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
