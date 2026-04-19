package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.repository.AlbumRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetAlbumsUseCaseTest {

    private lateinit var repository: AlbumRepository
    private lateinit var useCase: GetAlbumsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetAlbumsUseCase(repository)
    }

    @Test
    fun `invoke returns albums from repository`() = runTest {
        val albums = listOf(
            Album(1L, "A", "", "X", "2000", "Rock"),
            Album(2L, "B", "", "Y", "2001", "Jazz"),
        )
        coEvery { repository.getAlbums() } returns albums

        val result = useCase()

        assertEquals(albums, result)
        coVerify(exactly = 1) { repository.getAlbums() }
    }

    @Test
    fun `invoke returns empty list when repository has none`() = runTest {
        coEvery { repository.getAlbums() } returns emptyList()

        val result = useCase()

        assertEquals(0, result.size)
    }

    @Test(expected = IOException::class)
    fun `invoke propagates repository exception`() = runTest {
        coEvery { repository.getAlbums() } throws IOException("offline")

        useCase()
    }
}
