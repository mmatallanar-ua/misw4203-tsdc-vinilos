package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary

interface CollectorRepository {
    suspend fun getCollectors(): List<CollectorSummary>
    suspend fun getCollectorDetail(id: Int): CollectorDetail
}
