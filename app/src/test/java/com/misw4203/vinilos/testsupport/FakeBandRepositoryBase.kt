package com.misw4203.vinilos.testsupport

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository

/**
 * Base de fake reutilizable para BandRepository en tests unitarios.
 * Los métodos de negocio lanzan error() para que el test falle claramente si se
 * invocan sin sobrescribir. Los ex-métodos no-op son explícitamente vacíos.
 */
open class FakeBandRepositoryBase : BandRepository {

    override suspend fun getBands(): List<BandSummary> =
        error("override in test: getBands")

    override suspend fun getBandDetail(id: Int): Band =
        error("override in test: getBandDetail")

    override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) =
        error("override in test: addMusicianToBand")

    // no-op de test; sobrescribir si se ejercita
    override suspend fun addAlbumToBand(bandId: Int, albumId: Long) {}

    // no-op de test; sobrescribir si se ejercita
    override suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) {}
}
