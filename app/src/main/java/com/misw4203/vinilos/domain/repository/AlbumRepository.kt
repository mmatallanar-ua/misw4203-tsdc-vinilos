package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Track

interface AlbumRepository {
    suspend fun getAlbums(): List<Album>
    suspend fun getAlbumById(id: Long): AlbumDetail
    suspend fun addTrack(albumId: Long, request: CreateTrackRequest): Track
}
