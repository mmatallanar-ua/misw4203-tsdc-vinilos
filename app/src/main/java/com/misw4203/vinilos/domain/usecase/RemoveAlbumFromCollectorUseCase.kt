package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class RemoveAlbumFromCollectorUseCase @Inject constructor(
    private val repository: CollectorRepository,
) {
    suspend operator fun invoke(collectorId: Int, albumId: Long) =
        repository.removeAlbumFromCollector(collectorId, albumId)
}
