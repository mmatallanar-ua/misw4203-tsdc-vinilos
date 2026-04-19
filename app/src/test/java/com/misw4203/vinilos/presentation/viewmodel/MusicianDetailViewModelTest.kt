package com.misw4203.vinilos.presentation.viewmodel

import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MusicianDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun buildViewModel(repo: MusicianRepository) =
        MusicianDetailViewModel(GetMusicianDetailUseCase(repo))

    private fun sampleMusician(id: Int = 2) = Musician(
        id = id,
        name = "Rubén Blades",
        image = "img",
        description = "desc",
        birthDate = "1948-07-16",
        albums = emptyList(),
        prizes = emptyList(),
    )

    @Test
    fun `initial state is Loading`() = runTest {
        val repo = mockk<MusicianRepository>()
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMusician emits Success when repository returns musician`() = runTest {
        val repo = mockk<MusicianRepository>().also {
            coEvery { it.getMusicianDetail(2) } returns sampleMusician()
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            viewModel.loadMusician(2)
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is MusicianDetailUiState.Success)
            assertEquals("Rubén Blades", (state as MusicianDetailUiState.Success).musician.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMusician emits Error when repository throws`() = runTest {
        val repo = mockk<MusicianRepository>().also {
            coEvery { it.getMusicianDetail(99) } throws IOException("offline")
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            viewModel.loadMusician(99)
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is MusicianDetailUiState.Error)
            assertEquals("offline", (state as MusicianDetailUiState.Error).message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMusician emits Loading before Success on subsequent calls`() = runTest {
        val repo = mockk<MusicianRepository>().also {
            coEvery { it.getMusicianDetail(2) } returns sampleMusician(id = 2)
            coEvery { it.getMusicianDetail(3) } returns sampleMusician(id = 3).copy(name = "Otro")
        }
        val viewModel = buildViewModel(repo)

        viewModel.uiState.test {
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            viewModel.loadMusician(2)
            advanceUntilIdle()
            assertTrue(awaitItem() is MusicianDetailUiState.Success)

            viewModel.loadMusician(3)
            assertEquals(MusicianDetailUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertEquals("Otro", (state as MusicianDetailUiState.Success).musician.name)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
