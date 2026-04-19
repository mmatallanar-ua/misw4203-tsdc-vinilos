package com.misw4203.vinilos.data.repository

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
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: VinilosApiService,
) : AlbumRepository {

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        api.getAlbums().map { it.toAlbum() }
    }

    override suspend fun getAlbumById(id: Long): AlbumDetail = withContext(Dispatchers.IO) {
        api.getAlbum(id).toAlbumDetail()
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
