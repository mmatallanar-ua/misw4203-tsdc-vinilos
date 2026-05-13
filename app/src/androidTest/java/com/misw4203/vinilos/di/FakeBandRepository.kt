package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class FakeBandRepository @Inject constructor() : BandRepository {

    override suspend fun getBands(): List<BandSummary> = listOf(
        BandSummary(1, "Queen", ""),
        BandSummary(2, "The Beatles", ""),
    )

    override suspend fun getBandDetail(id: Int): Band = Band(
        id = id,
        name = "Queen",
        image = "",
        description = "Banda de rock británica formada en Londres en 1970.",
        creationDate = "1970-01-01T00:00:00.000Z",
        members = emptyList(),
        albums = emptyList(),
    )

    override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) {
        // no-op
    }
}
