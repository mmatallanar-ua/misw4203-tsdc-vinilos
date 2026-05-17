package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.model.NewTrack
import com.misw4203.vinilos.domain.model.Track

interface AlbumRepository {
    suspend fun getAlbums(): List<Album>
    suspend fun getAlbumById(id: Long): AlbumDetail
    suspend fun addTrack(albumId: Long, track: NewTrack): Track
    suspend fun addComment(albumId: Long, description: String, rating: Int, collectorId: Int): Comment
    suspend fun createAlbum(input: CreateAlbumInput): Album

    // Removal operations. Default no-op keeps in-memory test fakes compiling;
    // AlbumRepositoryImpl provides the real behaviour.
    suspend fun removeTrack(albumId: Long, trackId: Long) = Unit
    suspend fun removeComment(albumId: Long, commentId: Long) = Unit

    suspend fun addMusicianToAlbum(albumId: Long, musicianId: Int) = Unit
    suspend fun addBandToAlbum(albumId: Long, bandId: Int) = Unit
}
