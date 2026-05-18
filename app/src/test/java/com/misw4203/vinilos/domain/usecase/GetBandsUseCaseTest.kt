package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBandsUseCaseTest {

    private val repo: BandRepository = mockk()
    private val useCase = GetBandsUseCase(repo)

    @Test
    fun `invoke delegates to repository getBands`() = runTest {
        val expected = listOf(BandSummary(1, "Queen", "img"))
        coEvery { repo.getBands() } returns expected

        val result = useCase()

        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getBands() }
    }
}
