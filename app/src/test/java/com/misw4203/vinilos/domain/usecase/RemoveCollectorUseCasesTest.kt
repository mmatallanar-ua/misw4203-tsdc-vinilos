package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.CollectorRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoveCollectorUseCasesTest {

    private val repository: CollectorRepository = mockk(relaxed = true)

    @Test
    fun `RemoveFavoriteMusicianUseCase delegates to repository`() = runTest {
        RemoveFavoriteMusicianUseCase(repository).invoke(100, 7)
        coVerify(exactly = 1) { repository.removeFavoriteMusician(100, 7) }
    }

    @Test
    fun `RemoveFavoriteBandUseCase delegates to repository`() = runTest {
        RemoveFavoriteBandUseCase(repository).invoke(100, 3)
        coVerify(exactly = 1) { repository.removeFavoriteBand(100, 3) }
    }

    @Test
    fun `RemoveAlbumFromCollectorUseCase delegates to repository`() = runTest {
        RemoveAlbumFromCollectorUseCase(repository).invoke(100, 8)
        coVerify(exactly = 1) { repository.removeAlbumFromCollector(100, 8) }
    }
}
