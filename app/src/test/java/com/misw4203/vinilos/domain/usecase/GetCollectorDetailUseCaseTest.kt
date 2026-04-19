package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Collector
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

class GetCollectorDetailUseCaseTest {

    private lateinit var repository: CollectorRepository
    private lateinit var useCase: GetCollectorDetailUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCollectorDetailUseCase(repository)
    }

    @Test
    fun `invoke returns collector for given id from repository`() = runTest {
        val collector = Collector(
            id = 100,
            name = "Manolo",
            telephone = "3500",
            email = "m@a.co",
            comments = emptyList(),
            favoritePerformers = emptyList(),
            collectorAlbums = emptyList(),
        )
        coEvery { repository.getCollectorById(100) } returns collector

        val result = useCase(100)

        assertEquals(collector, result)
        coVerify(exactly = 1) { repository.getCollectorById(100) }
    }

    @Test(expected = HttpException::class)
    fun `invoke propagates HttpException`() = runTest {
        coEvery { repository.getCollectorById(any()) } throws HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )

        useCase(999)
    }
}
