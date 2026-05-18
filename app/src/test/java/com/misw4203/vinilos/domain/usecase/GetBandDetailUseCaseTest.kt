package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetBandDetailUseCaseTest {

    private val repo: BandRepository = mockk()
    private val useCase = GetBandDetailUseCase(repo)

    @Test
    fun `invoke delegates to repository getBandDetail with id`() = runTest {
        val expected = Band(
            id = 1, name = "Queen", image = "img", description = "desc",
            creationDate = "1970-01-01", members = emptyList(), albums = emptyList(),
        )
        coEvery { repo.getBandDetail(1) } returns expected

        val result = useCase(1)

        assertEquals(expected, result)
        coVerify(exactly = 1) { repo.getBandDetail(1) }
    }
}
