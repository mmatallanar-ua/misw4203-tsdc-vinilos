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

    suspend fun removeTrack(albumId: Long, trackId: Long)
    suspend fun removeComment(albumId: Long, commentId: Long)
    suspend fun addMusicianToAlbum(albumId: Long, musicianId: Int)
    suspend fun addBandToAlbum(albumId: Long, bandId: Int)
}
