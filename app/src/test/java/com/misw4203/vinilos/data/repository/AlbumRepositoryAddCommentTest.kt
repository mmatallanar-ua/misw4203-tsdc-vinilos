package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.CreateCommentRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AlbumRepositoryAddCommentTest {

    private lateinit var api: VinilosApiService
    private lateinit var dao: AlbumDao
    private lateinit var repository: AlbumRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = AlbumRepositoryImpl(api, dao)
    }

    @Test
    fun `addComment posts request with collector ref and returns mapped Comment`() = runTest {
        val capturedRequest = slot<CreateCommentRequest>()
        coEvery { api.addComment(albumId = 1L, request = capture(capturedRequest)) } returns CommentDto(
            id = 12L,
            description = "Excelente álbum",
            rating = 5,
        )

        val result = repository.addComment(
            albumId = 1L,
            description = "Excelente álbum",
            rating = 5,
            collectorId = 100,
        )

        assertEquals(12L, result.id)
        assertEquals("Excelente álbum", result.description)
        assertEquals(5, result.rating)

        val sent = capturedRequest.captured
        assertEquals("Excelente álbum", sent.description)
        assertEquals(5, sent.rating)
        assertEquals(100, sent.collector.id)

        coVerify(exactly = 1) { api.addComment(albumId = 1L, request = any()) }
    }

    @Test
    fun `addComment falls back to submitted rating when response rating is null`() = runTest {
        coEvery { api.addComment(any(), any()) } returns CommentDto(
            id = 7L,
            description = "Texto",
            rating = null,
        )

        val result = repository.addComment(albumId = 2L, description = "Texto", rating = 4, collectorId = 100)

        assertEquals(4, result.rating)
    }

    @Test(expected = IOException::class)
    fun `addComment propagates IOException`() = runTest {
        coEvery { api.addComment(any(), any()) } throws IOException("offline")

        repository.addComment(albumId = 1L, description = "x", rating = 3, collectorId = 100)
    }

    @Test(expected = HttpException::class)
    fun `addComment propagates HttpException`() = runTest {
        coEvery { api.addComment(any(), any()) } throws HttpException(
            Response.error<Any>(400, "".toResponseBody("text/plain".toMediaType()))
        )

        repository.addComment(albumId = 1L, description = "x", rating = 3, collectorId = 100)
    }
}
