package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import javax.inject.Inject

class GetMusiciansUseCase @Inject constructor(
    private val repository: MusicianRepository,
) {
    suspend operator fun invoke(): List<MusicianSummary> = repository.getMusicians()
}
