package com.misw4203.vinilos.data.remote.api

import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PrizeDetailDto
import retrofit2.http.GET
import retrofit2.http.Path

interface VinilosApiService {

    @GET("musicians/{id}")
    suspend fun getMusicianDetail(@Path("id") id: Int): MusicianDetailDto

    @GET("prizes/{id}")
    suspend fun getPrizeDetail(@Path("id") id: Int): PrizeDetailDto
}
