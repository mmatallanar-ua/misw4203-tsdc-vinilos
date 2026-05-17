package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class RemoveCommentUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: Long, commentId: Long) =
        repository.removeComment(albumId, commentId)
}
