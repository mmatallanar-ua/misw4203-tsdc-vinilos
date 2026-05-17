package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary

interface CollectorRepository {
    suspend fun getCollectors(): List<CollectorSummary>
    suspend fun getCollectorDetail(id: Int): CollectorDetail
    suspend fun addAlbumToCollector(collectorId: Int, albumId: Int, price: Double, status: String)
    suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int)
    suspend fun addFavoriteBand(collectorId: Int, bandId: Int)

    // Removal operations. Default no-op keeps the many in-memory test fakes
    // compiling; CollectorRepositoryImpl provides the real behaviour.
    suspend fun removeFavoriteMusician(collectorId: Int, musicianId: Int) = Unit
    suspend fun removeFavoriteBand(collectorId: Int, bandId: Int) = Unit
    suspend fun removeAlbumFromCollector(collectorId: Int, albumId: Int) = Unit
}
