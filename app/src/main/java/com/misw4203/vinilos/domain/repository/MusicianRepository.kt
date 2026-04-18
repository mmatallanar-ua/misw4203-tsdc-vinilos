package com.misw4203.vinilos.domain.repository

import com.misw4203.vinilos.domain.model.Musician

interface MusicianRepository {
    suspend fun getMusicianDetail(id: Int): Musician
}
