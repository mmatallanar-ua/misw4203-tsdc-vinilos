package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class AddMusicianToAlbumUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(albumId: Long, musicianId: Int) =
        repository.addMusicianToAlbum(albumId, musicianId)
}
