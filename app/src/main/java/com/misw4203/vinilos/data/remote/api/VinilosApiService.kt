package com.misw4203.vinilos.data.remote.api

import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorDetailDto
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.CreateCommentRequest
import com.misw4203.vinilos.data.remote.dto.CreateAlbumRequestDto
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PrizeDetailDto
import com.misw4203.vinilos.data.remote.dto.TrackDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VinilosApiService {

    @GET("albums")
    suspend fun getAlbums(): List<AlbumDto>

    @GET("albums/{id}")
    suspend fun getAlbum(@Path("id") id: Long): AlbumDto

    @POST("albums")
    suspend fun createAlbum(@Body body: CreateAlbumRequestDto): AlbumDto

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

    @POST("albums/{id}/tracks")
    suspend fun addTrack(
        @Path("id") albumId: Long,
        @Body request: CreateTrackRequest,
    ): TrackDto

    @POST("albums/{id}/comments")
    suspend fun addComment(
        @Path("id") albumId: Long,
        @Body request: CreateCommentRequest,
    ): CommentDto
}
