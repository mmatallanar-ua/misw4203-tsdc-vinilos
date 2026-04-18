package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.remote.api.VinilosApi
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.repository.AlbumRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val api: VinilosApi,
) : AlbumRepository {

    override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        api.getAlbums().map { it.toDomain() }
    }

    private fun AlbumDto.toDomain(): Album = Album(
        id = id,
        name = name.orEmpty(),
        coverUrl = cover.orEmpty(),
        artistName = performers?.firstOrNull()?.name.orEmpty(),
        releaseYear = releaseDate?.take(4).orEmpty(),
        genre = genre.orEmpty(),
    )
}
