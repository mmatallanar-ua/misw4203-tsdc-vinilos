package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(
        albumId: Long,
        description: String,
        rating: Int,
        collectorId: Int,
    ): Comment = repository.addComment(albumId, description, rating, collectorId)
}
