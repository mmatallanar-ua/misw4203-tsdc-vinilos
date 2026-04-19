package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class GetAlbumDetailUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(id: Long): AlbumDetail = repository.getAlbumById(id)
}
