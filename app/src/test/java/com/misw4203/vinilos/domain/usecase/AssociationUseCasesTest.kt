package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.repository.BandRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AssociationUseCasesTest {

    private val albumRepo: AlbumRepository = mockk(relaxed = true)
    private val bandRepo: BandRepository = mockk(relaxed = true)

    @Test
    fun `AddMusicianToAlbumUseCase delegates`() = runTest {
        AddMusicianToAlbumUseCase(albumRepo).invoke(5L, 7)
        coVerify(exactly = 1) { albumRepo.addMusicianToAlbum(5L, 7) }
    }

    @Test
    fun `AddBandToAlbumUseCase delegates`() = runTest {
        AddBandToAlbumUseCase(albumRepo).invoke(5L, 3)
        coVerify(exactly = 1) { albumRepo.addBandToAlbum(5L, 3) }
    }

    @Test
    fun `AddAlbumToBandUseCase delegates`() = runTest {
        AddAlbumToBandUseCase(bandRepo).invoke(3, 5L)
        coVerify(exactly = 1) { bandRepo.addAlbumToBand(3, 5L) }
    }

    @Test
    fun `AddPrizeToBandUseCase delegates`() = runTest {
        AddPrizeToBandUseCase(bandRepo).invoke(3, 10, "2024-01-01T00:00:00.000Z")
        coVerify(exactly = 1) { bandRepo.addPrizeToBand(3, 10, "2024-01-01T00:00:00.000Z") }
    }
}
