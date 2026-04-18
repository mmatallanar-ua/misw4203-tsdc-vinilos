package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.repository.MusicianRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetMusicianDetailUseCaseTest {

    private lateinit var repository: MusicianRepository
    private lateinit var useCase: GetMusicianDetailUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetMusicianDetailUseCase(repository)
    }

    @Test
    fun `given a valid musician id, when invoke is called, then returns the musician from repository`() = runTest {
        // Given
        val musicianId = 2
        val expectedMusician = aMusician(id = musicianId)
        coEvery { repository.getMusicianDetail(musicianId) } returns expectedMusician

        // When
        val result = useCase(musicianId)

        // Then
        assertEquals(expectedMusician, result)
        coVerify(exactly = 1) { repository.getMusicianDetail(musicianId) }
    }

    @Test
    fun `given a valid musician id, when invoke is called, then musician name matches`() = runTest {
        // Given
        val musicianId = 2
        val expectedMusician = aMusician(id = musicianId, name = "Rubén Blades Bellido de Luna")
        coEvery { repository.getMusicianDetail(musicianId) } returns expectedMusician

        // When
        val result = useCase(musicianId)

        // Then
        assertEquals("Rubén Blades Bellido de Luna", result.name)
    }

    @Test
    fun `given a valid musician id, when invoke is called, then musician albums are returned`() = runTest {
        // Given
        val musicianId = 2
        val albums = listOf(anAlbum(id = 1, name = "Buscando América"))
        val expectedMusician = aMusician(id = musicianId, albums = albums)
        coEvery { repository.getMusicianDetail(musicianId) } returns expectedMusician

        // When
        val result = useCase(musicianId)

        // Then
        assertEquals(1, result.albums.size)
        assertEquals("Buscando América", result.albums[0].name)
    }

    @Test
    fun `given repository throws exception, when invoke is called, then exception is propagated`() = runTest {
        // Given
        val musicianId = 99
        coEvery { repository.getMusicianDetail(musicianId) } throws RuntimeException("Network error")

        // When / Then
        var thrownException: Exception? = null
        try {
            useCase(musicianId)
        } catch (e: Exception) {
            thrownException = e
        }
        assertEquals("Network error", thrownException?.message)
    }

    private fun aMusician(
        id: Int = 1,
        name: String = "Test Musician",
        albums: List<Album> = emptyList(),
        prizes: List<MusicianPrize> = emptyList()
    ) = Musician(
        id = id,
        name = name,
        image = "https://image.url",
        description = "Test description",
        birthDate = "1948-07-16T05:00:00.000Z",
        albums = albums,
        prizes = prizes
    )

    private fun anAlbum(
        id: Int = 1,
        name: String = "Test Album"
    ) = Album(
        id = id,
        name = name,
        cover = "https://cover.url",
        releaseDate = "1984-08-01T05:00:00.000Z",
        description = "Test album description",
        genre = "Salsa",
        recordLabel = "Elektra"
    )
}
