package com.misw4203.vinilos.data.remote.api

import com.misw4203.vinilos.data.remote.dto.AlbumDto
import retrofit2.http.GET
import retrofit2.http.Path

interface VinilosApi {
    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") id: Long): AlbumDto
}
