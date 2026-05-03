package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.domain.model.Track
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

class AddTrackUseCaseTest {

    private lateinit var repository: AlbumRepository
    private lateinit var useCase: AddTrackUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = AddTrackUseCase(repository)
    }

    @Test
    fun `invoke returns track from repository`() = runTest {
        val request = CreateTrackRequest("Get Lucky", "04:08")
        val expected = Track(1L, "Get Lucky", "04:08")
        coEvery { repository.addTrack(100L, request) } returns expected

        val result = useCase(100L, request)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repository.addTrack(100L, request) }
    }

    @Test(expected = IOException::class)
    fun `invoke propagates IOException`() = runTest {
        coEvery { repository.addTrack(any(), any()) } throws IOException("offline")
        useCase(100L, CreateTrackRequest("X", "01:00"))
    }

    @Test(expected = HttpException::class)
    fun `invoke propagates HttpException`() = runTest {
        coEvery { repository.addTrack(any(), any()) } throws HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        useCase(999L, CreateTrackRequest("X", "01:00"))
    }
}
