package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class FakeCollectorRepository @Inject constructor() : CollectorRepository {

    override suspend fun getCollectors(): List<CollectorSummary> = listOf(
        CollectorSummary(1, "Jaime Andrés Monsalve", "3102178976", "j.monsalve@gmail.com"),
        CollectorSummary(2, "María Alejandra Palacios", "3502889087", "j.palacios@outlook.es"),
    )
}
