package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.NewTrack
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class AddTrackUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: Long, track: NewTrack): Track =
        repository.addTrack(albumId, track)
}
