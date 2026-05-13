package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary

interface BandRepository {
    suspend fun getBands(): List<BandSummary>
    suspend fun getBandDetail(id: Int): Band
    suspend fun addMusicianToBand(bandId: Int, musicianId: Int)
}
