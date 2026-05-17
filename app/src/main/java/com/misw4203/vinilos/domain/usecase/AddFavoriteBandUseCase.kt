package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class AddFavoriteBandUseCase @Inject constructor(
    private val repository: CollectorRepository,
) {
    suspend operator fun invoke(collectorId: Int, bandId: Int) =
        repository.addFavoriteBand(collectorId, bandId)
}
