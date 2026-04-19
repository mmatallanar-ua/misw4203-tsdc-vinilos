package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.model.CollectorSummary

interface CollectorRepository {
    suspend fun getCollectors(): List<CollectorSummary>
    suspend fun getCollectorById(id: Int): Collector
}
