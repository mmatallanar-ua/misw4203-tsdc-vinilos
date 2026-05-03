package com.misw4203.vinilos.di

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.repository.CollectorRepository
import javax.inject.Inject

class FakeCollectorRepository @Inject constructor() : CollectorRepository {

    override suspend fun getCollectors(): List<CollectorSummary> = listOf(
        CollectorSummary(1, "Jaime Andrés Monsalve", "3102178976", "j.monsalve@gmail.com"),
        CollectorSummary(2, "María Alejandra Palacios", "3502889087", "j.palacios@outlook.es"),
    )

    override suspend fun getCollectorDetail(id: Int): CollectorDetail = CollectorDetail(
        id = id,
        name = "Jaime Andrés Monsalve",
        telephone = "3102178976",
        email = "j.monsalve@gmail.com",
        description = "Coleccionista apasionado de salsa y música latina.",
        collectorAlbums = listOf(
            CollectorAlbum(
                id = 1,
                price = 35.0,
                status = "Active",
                album = Album(
                    id = 1L,
                    name = "Buscando América",
                    coverUrl = "https://i.pinimg.com/564x/aa/5f/ed/aa5fed7fac61cc8f41d1e79db917a7cd.jpg",
                    artistName = "Rubén Blades",
                    releaseYear = "1984",
                    genre = "Salsa",
                ),
            ),
        ),
        favoritePerformers = listOf(
            Performer(
                id = 1L,
                name = "Rubén Blades Bellido de Luna",
                imageUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bb/Ruben_Blades_by_Gage_Skidmore.jpg/800px-Ruben_Blades_by_Gage_Skidmore.jpg",
            ),
        ),
        comments = listOf(
            CollectorComment(1L, "Edición impecable, sin ruidos de fondo.", 5, "Buscando América"),
        ),
    )
}
