package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class AddAlbumToCollectorUseCase @Inject constructor(
    private val repository: CollectorRepository,
) {
    suspend operator fun invoke(collectorId: Int, albumId: Long, price: Double, status: String) =
        repository.addAlbumToCollector(collectorId, albumId, price, status)
}
