package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class FakeAlbumRepository @Inject constructor() : AlbumRepository {

    override suspend fun getAlbums(): List<Album> = listOf(
        Album(1L, "Buscando América", "", "Rubén Blades", "1984", "Salsa"),
        Album(2L, "A Night at the Opera", "", "Queen", "1975", "Rock"),
    )

    override suspend fun addTrack(albumId: Long, request: CreateTrackRequest): Track =
        Track(id = 999L, name = request.name, duration = request.duration)

    override suspend fun getAlbumById(id: Long): AlbumDetail = AlbumDetail(
        id = id,
        name = "Buscando América",
        coverUrl = "",
        artistName = "Rubén Blades",
        releaseDate = "1984-01-01",
        genre = "Salsa",
        recordLabel = "Elektra",
        description = "Álbum debut de Rubén Blades con Seis del Solar.",
        tracks = listOf(Track(1L, "Decisiones", "5:12")),
        performers = listOf(Performer(1L, "Rubén Blades", "")),
        comments = listOf(Comment(1L, "Gran álbum", 5)),
    )
}
