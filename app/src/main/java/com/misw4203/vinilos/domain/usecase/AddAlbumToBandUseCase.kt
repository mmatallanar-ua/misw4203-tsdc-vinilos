package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.BandRepository
import javax.inject.Inject

class AddAlbumToBandUseCase @Inject constructor(
    private val repository: BandRepository,
) {
    suspend operator fun invoke(bandId: Int, albumId: Long) =
        repository.addAlbumToBand(bandId, albumId)
}
