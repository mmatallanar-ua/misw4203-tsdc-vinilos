package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class AddMusicianToBandUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(bandId: Int, musicianId: Int) =
        repository.addMusicianToBand(bandId, musicianId)
}
