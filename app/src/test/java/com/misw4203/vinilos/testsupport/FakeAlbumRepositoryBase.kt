package com.misw4203.vinilos.testsupport

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.model.NewTrack
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository

/**
 * Base de fake reutilizable para AlbumRepository en tests unitarios.
 * Los métodos de negocio lanzan error() para que el test falle claramente si se
 * invocan sin sobrescribir. Los ex-métodos no-op son explícitamente vacíos.
 */
open class FakeAlbumRepositoryBase : AlbumRepository {

    override suspend fun getAlbums(): List<Album> =
        error("override in test: getAlbums")

    override suspend fun getAlbumById(id: Long): AlbumDetail =
        error("override in test: getAlbumById")

    override suspend fun addTrack(albumId: Long, track: NewTrack): Track =
        error("override in test: addTrack")

    override suspend fun addComment(
        albumId: Long,
        description: String,
        rating: Int,
        collectorId: Int,
    ): Comment = error("override in test: addComment")

    override suspend fun createAlbum(input: CreateAlbumInput): Album =
        error("override in test: createAlbum")

    // no-op de test; sobrescribir si se ejercita
    override suspend fun removeTrack(albumId: Long, trackId: Long) {}

    // no-op de test; sobrescribir si se ejercita
    override suspend fun removeComment(albumId: Long, commentId: Long) {}

    // no-op de test; sobrescribir si se ejercita
    override suspend fun addMusicianToAlbum(albumId: Long, musicianId: Int) {}

    // no-op de test; sobrescribir si se ejercita
    override suspend fun addBandToAlbum(albumId: Long, bandId: Int) {}
}
