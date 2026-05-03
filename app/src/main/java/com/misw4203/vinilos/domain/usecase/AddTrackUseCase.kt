package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class AddTrackUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: Long, request: CreateTrackRequest): Track =
        repository.addTrack(albumId, request)
}
