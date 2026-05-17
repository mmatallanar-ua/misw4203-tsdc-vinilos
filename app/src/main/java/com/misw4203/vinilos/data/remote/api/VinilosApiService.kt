package com.misw4203.vinilos.data.remote.api

import com.misw4203.vinilos.data.remote.dto.AddCollectorAlbumRequest
import com.misw4203.vinilos.data.remote.dto.AddPrizeToMusicianRequest
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.BandDetailDto
import com.misw4203.vinilos.data.remote.dto.BandDto
import com.misw4203.vinilos.data.remote.dto.CollectorAlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorDetailDto
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.CreateCommentRequest
import com.misw4203.vinilos.data.remote.dto.CreateAlbumRequestDto
import com.misw4203.vinilos.data.remote.dto.CreatePrizeRequest
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDto
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

    @GET("prizes")
    suspend fun getPrizes(): List<PrizeDetailDto>

    @GET("prizes/{id}")
    suspend fun getPrizeDetail(@Path("id") id: Int): PrizeDetailDto

    @GET("performerprizes")
    suspend fun getPerformerPrizes(): List<PerformerPrizeDetailDto>

    @POST("prizes")
    suspend fun createPrize(@Body body: CreatePrizeRequest): PrizeDetailDto

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

    @GET("bands")
    suspend fun getBands(): List<BandDto>

    @GET("bands/{id}")
    suspend fun getBandDetail(@Path("id") id: Int): BandDetailDto

    @POST("bands/{bandId}/musicians/{musicianId}")
    suspend fun addMusicianToBand(
        @Path("bandId") bandId: Int,
        @Path("musicianId") musicianId: Int,
    )

    @GET("collectors/{collectorId}/albums")
    suspend fun getCollectorAlbums(
        @Path("collectorId") collectorId: Int,
    ): List<CollectorAlbumDto>

    @POST("collectors/{collectorId}/albums/{albumId}")
    suspend fun addAlbumToCollector(
        @Path("collectorId") collectorId: Int,
        @Path("albumId") albumId: Int,
        @Body request: AddCollectorAlbumRequest,
    ): CollectorAlbumDto

    @POST("musicians/{musicianId}/albums/{albumId}")
    suspend fun addAlbumToMusician(
        @Path("musicianId") musicianId: Int,
        @Path("albumId") albumId: Int,
    )

    @POST("prizes/{prizeId}/musicians/{musicianId}")
    suspend fun addPrizeToMusician(
        @Path("prizeId") prizeId: Int,
        @Path("musicianId") musicianId: Int,
        @Body request: AddPrizeToMusicianRequest,
    ): PerformerPrizeDto

    @POST("collectors/{collectorId}/musicians/{musicianId}")
    suspend fun addMusicianToCollector(
        @Path("collectorId") collectorId: Int,
        @Path("musicianId") musicianId: Int,
    )

    @POST("collectors/{collectorId}/bands/{bandId}")
    suspend fun addBandToCollector(
        @Path("collectorId") collectorId: Int,
        @Path("bandId") bandId: Int,
    )
}
