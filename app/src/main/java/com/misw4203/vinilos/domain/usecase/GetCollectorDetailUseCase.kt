package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class GetCollectorDetailUseCase @Inject constructor(
    private val repository: CollectorRepository,
) {
    suspend operator fun invoke(id: Int): CollectorDetail = repository.getCollectorDetail(id)
}
