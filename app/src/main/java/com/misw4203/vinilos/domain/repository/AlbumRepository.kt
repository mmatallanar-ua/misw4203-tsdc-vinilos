package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Album

interface AlbumRepository {
    suspend fun getAlbums(): List<Album>
}
