package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(): List<Album> = repository.getAlbums()
}
