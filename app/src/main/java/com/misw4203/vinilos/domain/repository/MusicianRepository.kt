package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary

interface MusicianRepository {
    suspend fun getMusicians(): List<MusicianSummary>
    suspend fun getMusicianDetail(id: Int): Musician
}
