package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class GetBandsUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(): List<BandSummary> = repository.getBands()
}
