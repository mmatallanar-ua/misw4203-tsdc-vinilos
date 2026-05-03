package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class GetCollectorsUseCase @Inject constructor(
    private val repository: CollectorRepository,
) {
    suspend operator fun invoke(): List<CollectorSummary> = repository.getCollectors()
}
