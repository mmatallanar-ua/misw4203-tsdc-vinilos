package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.repository.AlbumRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody

class GetAlbumDetailUseCaseTest {

    private lateinit var repository: AlbumRepository
    private lateinit var useCase: GetAlbumDetailUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetAlbumDetailUseCase(repository)
    }

    @Test
    fun `invoke returns detail for given id from repository`() = runTest {
        val detail = AlbumDetail(
            id = 42L,
            name = "OK Computer",
            coverUrl = "",
            artistName = "Radiohead",
            releaseDate = "1997-05-21",
            genre = "Rock",
            recordLabel = "Parlophone",
            description = "",
            tracks = emptyList(),
            performers = emptyList(),
            comments = emptyList(),
        )
        coEvery { repository.getAlbumById(42L) } returns detail

        val result = useCase(42L)

        assertEquals(detail, result)
        coVerify(exactly = 1) { repository.getAlbumById(42L) }
    }

    @Test(expected = HttpException::class)
    fun `invoke propagates HttpException`() = runTest {
        coEvery { repository.getAlbumById(any()) } throws HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )

        useCase(999L)
    }
}
