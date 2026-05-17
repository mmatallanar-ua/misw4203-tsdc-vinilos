package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.MusicianRepository
import javax.inject.Inject

class AddPrizeToMusicianUseCase @Inject constructor(
    private val repository: MusicianRepository,
) {
    suspend operator fun invoke(musicianId: Int, prizeId: Int, premiationDate: String) =
        repository.addPrizeToMusician(musicianId, prizeId, premiationDate)
}
