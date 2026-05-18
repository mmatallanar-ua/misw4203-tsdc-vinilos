package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary

interface BandRepository {
    suspend fun getBands(): List<BandSummary>
    suspend fun getBandDetail(id: Int): Band
    suspend fun addMusicianToBand(bandId: Int, musicianId: Int)

    // Default no-op keeps in-memory test fakes compiling; BandRepositoryImpl
    // provides the real behaviour.
    suspend fun addAlbumToBand(bandId: Int, albumId: Long) = Unit
    suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) = Unit
}
