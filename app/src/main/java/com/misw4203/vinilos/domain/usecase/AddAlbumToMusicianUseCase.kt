package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.MusicianRepository
import javax.inject.Inject

class AddAlbumToMusicianUseCase @Inject constructor(
    private val repository: MusicianRepository,
) {
    suspend operator fun invoke(musicianId: Int, albumId: Long) =
        repository.addAlbumToMusician(musicianId, albumId)
}
