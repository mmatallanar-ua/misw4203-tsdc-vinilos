package com.misw4203.vinilos.data.remote.api

import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorDetailDto
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PrizeDetailDto
import retrofit2.http.GET
import retrofit2.http.Path

interface VinilosApiService {

    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") id: Long): AlbumDto

    @GET("musicians")
    suspend fun getMusicians(): List<MusicianDetailDto>

    @GET("musicians/{id}")
    suspend fun getMusicianDetail(@Path("id") id: Int): MusicianDetailDto

    @GET("prizes/{id}")
    suspend fun getPrizeDetail(@Path("id") id: Int): PrizeDetailDto

    @GET("collectors")
    suspend fun getCollectors(): List<CollectorDto>

    @GET("collectors/{id}")
    suspend fun getCollectorDetail(@Path("id") id: Int): CollectorDetailDto
}
