package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.MusicianRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddPrizeToMusicianUseCaseTest {

    private val repo: MusicianRepository = mockk()
    private val useCase = AddPrizeToMusicianUseCase(repo)

    @Test
    fun `invoke delegates all args to repository`() = runTest {
        coEvery { repo.addPrizeToMusician(1, 10, "2020-01-01T00:00:00.000Z") } returns Unit

        useCase(musicianId = 1, prizeId = 10, premiationDate = "2020-01-01T00:00:00.000Z")

        coVerify(exactly = 1) { repo.addPrizeToMusician(1, 10, "2020-01-01T00:00:00.000Z") }
    }
}
