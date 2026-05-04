package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.repository.AlbumRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class CreateAlbumUseCaseTest {

    private lateinit var repository: AlbumRepository
    private lateinit var useCase: CreateAlbumUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = CreateAlbumUseCase(repository)
    }

    @Test
    fun `invoke calls repository createAlbum and returns album`() = runTest {
        val input = sampleInput()
        val expected = sampleAlbum()
        coEvery { repository.createAlbum(input) } returns expected

        val result = useCase(input)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.createAlbum(input) }
    }

    @Test
    fun `invoke returns album with correct fields`() = runTest {
        val input = sampleInput()
        val expected = sampleAlbum().copy(name = "Buscando América", genre = "Salsa")
        coEvery { repository.createAlbum(input) } returns expected

        val result = useCase(input)

        assertEquals("Buscando América", result.name)
        assertEquals("Salsa", result.genre)
    }

    @Test(expected = IOException::class)
    fun `invoke propagates IOException from repository`() = runTest {
        coEvery { repository.createAlbum(any()) } throws IOException("offline")
        useCase(sampleInput())
    }

    @Test(expected = RuntimeException::class)
    fun `invoke propagates generic exception from repository`() = runTest {
        coEvery { repository.createAlbum(any()) } throws RuntimeException("unexpected")
        useCase(sampleInput())
    }
}

private fun sampleInput() = CreateAlbumInput(
    name = "Test Album",
    cover = "https://example.com/cover.jpg",
    releaseDate = "2024-01-15",
    description = "Description",
    genre = "Rock",
    recordLabel = "Sony Music",
)

private fun sampleAlbum() = Album(
    id = 99L,
    name = "Test Album",
    coverUrl = "https://example.com/cover.jpg",
    artistName = "",
    releaseYear = "2024",
    genre = "Rock",
)
