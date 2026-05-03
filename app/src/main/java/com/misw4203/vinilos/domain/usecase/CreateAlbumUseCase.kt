package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.repository.AlbumRepository
import javax.inject.Inject

class CreateAlbumUseCase @Inject constructor(
    private val repository: AlbumRepository,
) {
    suspend operator fun invoke(input: CreateAlbumInput): Album = repository.createAlbum(input)
}
