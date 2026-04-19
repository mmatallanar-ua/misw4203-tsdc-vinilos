package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.CollectorSummary
import com.misw4203.vinilos.domain.repository.CollectorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetCollectorsUseCaseTest {

    private lateinit var repository: CollectorRepository
    private lateinit var useCase: GetCollectorsUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetCollectorsUseCase(repository)
    }

    @Test
    fun `invoke returns collectors from repository`() = runTest {
        val collectors = listOf(
            CollectorSummary(100, "Manolo", "m@a.co", "3500"),
            CollectorSummary(101, "Jaime", "j@a.co", "3012"),
        )
        coEvery { repository.getCollectors() } returns collectors

        val result = useCase()

        assertEquals(collectors, result)
        coVerify(exactly = 1) { repository.getCollectors() }
    }

    @Test
    fun `invoke returns empty list when repository has none`() = runTest {
        coEvery { repository.getCollectors() } returns emptyList()

        val result = useCase()

        assertEquals(0, result.size)
    }

    @Test(expected = IOException::class)
    fun `invoke propagates repository exception`() = runTest {
        coEvery { repository.getCollectors() } throws IOException("offline")

        useCase()
    }
}
