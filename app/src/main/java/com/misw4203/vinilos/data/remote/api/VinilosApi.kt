package com.misw4203.vinilos.data.remote.api

import com.misw4203.vinilos.data.remote.dto.AlbumDto
import retrofit2.http.GET

interface VinilosApi {
    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>
}
