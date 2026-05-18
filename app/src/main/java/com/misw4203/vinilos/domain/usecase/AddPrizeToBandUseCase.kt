package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class AddPrizeToBandUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(bandId: Int, prizeId: Int, premiationDate: String) =
        repository.addPrizeToBand(bandId, prizeId, premiationDate)
}
