package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.repository.MusicianRepository
import javax.inject.Inject

class GetMusicianDetailUseCase @Inject constructor(
    private val repository: MusicianRepository
) {
    suspend operator fun invoke(id: Int): Musician {
        return repository.getMusicianDetail(id)
    }
}
