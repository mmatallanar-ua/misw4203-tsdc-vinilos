package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary

interface CollectorRepository {
    suspend fun getCollectors(): List<CollectorSummary>
    suspend fun getCollectorDetail(id: Int): CollectorDetail
    suspend fun addAlbumToCollector(collectorId: Int, albumId: Long, price: Double, status: String)
    suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int)
    suspend fun addFavoriteBand(collectorId: Int, bandId: Int)

    suspend fun removeFavoriteMusician(collectorId: Int, musicianId: Int)
    suspend fun removeFavoriteBand(collectorId: Int, bandId: Int)
    suspend fun removeAlbumFromCollector(collectorId: Int, albumId: Long)
}
