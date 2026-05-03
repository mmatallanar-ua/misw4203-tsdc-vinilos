package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.repository.CollectorRepository
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

class GetCollectorDetailUseCaseTest {

    private lateinit var repository: CollectorRepository
    private lateinit var useCase: GetCollectorDetailUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCollectorDetailUseCase(repository)
    }

    @Test
    fun `invoke returns collector detail for given id`() = runTest {
        val detail = sampleDetail()
        coEvery { repository.getCollectorDetail(100) } returns detail

        val result = useCase(100)

        assertEquals(detail, result)
        coVerify(exactly = 1) { repository.getCollectorDetail(100) }
    }

    @Test(expected = HttpException::class)
    fun `invoke propagates HttpException`() = runTest {
        coEvery { repository.getCollectorDetail(any()) } throws HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        useCase(999)
    }

    @Test(expected = IOException::class)
    fun `invoke propagates IOException`() = runTest {
        coEvery { repository.getCollectorDetail(any()) } throws IOException("offline")
        useCase(100)
    }
}

private fun sampleDetail() = CollectorDetail(
    id = 100,
    name = "Manolo Bellon",
    telephone = "3502457896",
    email = "manollo@caracol.com.co",
    description = "",
    collectorAlbums = emptyList(),
    favoritePerformers = emptyList(),
    comments = emptyList(),
)
