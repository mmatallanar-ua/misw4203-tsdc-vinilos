package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class GetBandDetailUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(bandId: Int): Band = repository.getBandDetail(bandId)
}
