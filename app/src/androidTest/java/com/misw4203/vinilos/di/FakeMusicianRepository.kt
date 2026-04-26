package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import javax.inject.Inject

class FakeMusicianRepository @Inject constructor() : MusicianRepository {

    override suspend fun getMusicians(): List<MusicianSummary> = listOf(
        MusicianSummary(100, "Rubén Blades", "", "1948-07-16T00:00:00.000Z"),
        MusicianSummary(101, "Freddie Mercury", "", "1946-09-05T00:00:00.000Z"),
    )

    override suspend fun getMusicianDetail(id: Int): Musician = Musician(
        id = id,
        name = "Rubén Blades",
        image = "",
        description = "Cantante, compositor y político panameño.",
        birthDate = "1948-07-16T00:00:00.000Z",
        albums = listOf(Album(1L, "Buscando América", "", "Rubén Blades", "1984", "Salsa")),
        prizes = emptyList(),
    )
}
