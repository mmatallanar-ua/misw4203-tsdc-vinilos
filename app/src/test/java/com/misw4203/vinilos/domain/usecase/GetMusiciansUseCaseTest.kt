package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.MusicianRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class GetMusiciansUseCaseTest {

    private lateinit var repository: MusicianRepository
    private lateinit var useCase: GetMusiciansUseCase

    @Before
    fun setUp() {
        repository = mockk()
        useCase = GetMusiciansUseCase(repository)
    }

    @Test
    fun `invoke returns musicians from repository`() = runTest {
        val musicians = listOf(
            MusicianSummary(100, "Rubén Blades", "url1", "1948-07-16"),
            MusicianSummary(101, "Queen", "url2", ""),
        )
        coEvery { repository.getMusicians() } returns musicians

        val result = useCase()

        assertEquals(musicians, result)
        coVerify(exactly = 1) { repository.getMusicians() }
    }

    @Test
    fun `invoke returns empty list when repository has none`() = runTest {
        coEvery { repository.getMusicians() } returns emptyList()

        val result = useCase()

        assertEquals(0, result.size)
    }

    @Test(expected = IOException::class)
    fun `invoke propagates repository exception`() = runTest {
        coEvery { repository.getMusicians() } throws IOException("offline")

        useCase()
    }
}
