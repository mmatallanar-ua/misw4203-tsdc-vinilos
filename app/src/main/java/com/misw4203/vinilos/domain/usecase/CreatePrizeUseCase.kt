package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Prize
import com.misw4203.vinilos.domain.repository.PrizeRepository
import javax.inject.Inject

class CreatePrizeUseCase @Inject constructor(
    private val repository: PrizeRepository,
) {
    suspend operator fun invoke(name: String, description: String, organization: String): Prize =
        repository.createPrize(name, description, organization)
}
