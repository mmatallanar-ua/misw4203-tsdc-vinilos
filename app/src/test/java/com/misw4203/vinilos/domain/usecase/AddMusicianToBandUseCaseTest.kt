package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AddMusicianToBandUseCaseTest {

    private val repo: BandRepository = mockk()
    private val useCase = AddMusicianToBandUseCase(repo)

    @Test
    fun `invoke delegates bandId and musicianId to repository`() = runTest {
        coEvery { repo.addMusicianToBand(1, 10) } returns Unit

        useCase(bandId = 1, musicianId = 10)

        coVerify(exactly = 1) { repo.addMusicianToBand(1, 10) }
    }
}
