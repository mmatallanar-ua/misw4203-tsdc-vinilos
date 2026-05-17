package com.misw4203.vinilos.domain.usecase

import com.misw4203.vinilos.domain.repository.AlbumRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RemoveAlbumContentUseCasesTest {

    private val repository: AlbumRepository = mockk(relaxed = true)

    @Test
    fun `RemoveTrackUseCase delegates to repository`() = runTest {
        RemoveTrackUseCase(repository).invoke(5L, 1L)
        coVerify(exactly = 1) { repository.removeTrack(5L, 1L) }
    }

    @Test
    fun `RemoveCommentUseCase delegates to repository`() = runTest {
        RemoveCommentUseCase(repository).invoke(5L, 9L)
        coVerify(exactly = 1) { repository.removeComment(5L, 9L) }
    }
}
