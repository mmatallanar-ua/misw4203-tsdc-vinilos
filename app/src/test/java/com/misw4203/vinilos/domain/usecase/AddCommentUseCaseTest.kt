package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.repository.AlbumRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AddCommentUseCaseTest {

    private lateinit var repository: AlbumRepository
    private lateinit var useCase: AddCommentUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = AddCommentUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository and returns mapped Comment`() = runTest {
        val expected = Comment(id = 7L, description = "Buenísimo", rating = 5)
        coEvery {
            repository.addComment(albumId = 1L, description = "Buenísimo", rating = 5, collectorId = 100)
        } returns expected

        val result = useCase(albumId = 1L, description = "Buenísimo", rating = 5, collectorId = 100)

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            repository.addComment(1L, "Buenísimo", 5, 100)
        }
    }

    @Test(expected = IOException::class)
    fun `invoke propagates IOException from repository`() = runTest {
        coEvery { repository.addComment(any(), any(), any(), any()) } throws IOException("offline")

        useCase(1L, "comentario", 4, 100)
    }

    @Test(expected = HttpException::class)
    fun `invoke propagates HttpException from repository`() = runTest {
        coEvery { repository.addComment(any(), any(), any(), any()) } throws HttpException(
            Response.error<Any>(400, "".toResponseBody("text/plain".toMediaType()))
        )

        useCase(1L, "comentario", 4, 100)
    }
}
