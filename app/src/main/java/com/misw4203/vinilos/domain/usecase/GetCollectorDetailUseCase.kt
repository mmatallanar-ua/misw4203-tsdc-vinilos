package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class GetCollectorDetailUseCase @Inject constructor(
    private val repository: CollectorRepository,
) {
    suspend operator fun invoke(id: Int): Collector = repository.getCollectorById(id)
}
