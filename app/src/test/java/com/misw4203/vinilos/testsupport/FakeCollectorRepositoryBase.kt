package com.misw4203.vinilos.testsupport

import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository

/**
 * Base de fake reutilizable para CollectorRepository en tests unitarios.
 * Los métodos de negocio lanzan error() para que el test falle claramente si se
 * invocan sin sobrescribir. Los ex-métodos no-op son explícitamente vacíos.
 */
open class FakeCollectorRepositoryBase : CollectorRepository {

    override suspend fun getCollectors(): List<CollectorSummary> =
        error("override in test: getCollectors")

    override suspend fun getCollectorDetail(id: Int): CollectorDetail =
        error("override in test: getCollectorDetail")

    override suspend fun addAlbumToCollector(
        collectorId: Int,
        albumId: Int,
        price: Double,
        status: String,
    ) = error("override in test: addAlbumToCollector")

    override suspend fun addFavoriteMusician(collectorId: Int, musicianId: Int) =
        error("override in test: addFavoriteMusician")

    override suspend fun addFavoriteBand(collectorId: Int, bandId: Int) =
        error("override in test: addFavoriteBand")

    // no-op de test; sobrescribir si se ejercita
    override suspend fun removeFavoriteMusician(collectorId: Int, musicianId: Int) {}

    // no-op de test; sobrescribir si se ejercita
    override suspend fun removeFavoriteBand(collectorId: Int, bandId: Int) {}

    // no-op de test; sobrescribir si se ejercita
    override suspend fun removeAlbumFromCollector(collectorId: Int, albumId: Int) {}
}
