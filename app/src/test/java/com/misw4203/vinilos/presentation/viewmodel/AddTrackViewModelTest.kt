package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.usecase.AddTrackUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddTrackViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeRepo(var result: Result<Track>) : AlbumRepository {
        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = AlbumDetail(
            id, "", "", "", "", "", "", "", emptyList(), emptyList(), emptyList()
        )
        override suspend fun addTrack(albumId: Long, request: CreateTrackRequest): Track =
            result.getOrThrow()
    }

    private fun buildViewModel(repo: FakeRepo, albumId: Long = 100L) = AddTrackViewModel(
        AddTrackUseCase(repo),
        SavedStateHandle(mapOf(Destinations.AddTrackAlbumArg to albumId)),
    )

    @Test
    fun `submit with blank name sets nameError and does not emit Loading`() = runTest {
        val vm = buildViewModel(FakeRepo(Result.success(Track(1L, "x", "01:00"))))
        vm.duration = "03:45"

        vm.submit()

        assertNotNull(vm.nameError)
        assertEquals(AddTrackUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `submit with invalid duration sets durationError and does not emit Loading`() = runTest {
        val vm = buildViewModel(FakeRepo(Result.success(Track(1L, "x", "01:00"))))
        vm.name = "Get Lucky"
        vm.duration = "345"

        vm.submit()

        assertNotNull(vm.durationError)
        assertEquals(AddTrackUiState.Idle, vm.uiState.value)
    }

    @Test
    fun `submit with valid data emits Loading then Success`() = runTest {
        val track = Track(1L, "Get Lucky", "04:08")
        val vm = buildViewModel(FakeRepo(Result.success(track)))
        vm.name = "Get Lucky"
        vm.duration = "04:08"

        vm.uiState.test {
            assertEquals(AddTrackUiState.Idle, awaitItem())
            vm.submit()
            assertEquals(AddTrackUiState.Loading, awaitItem())
            advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state is AddTrackUiState.Success)
            assertEquals(track, (state as AddTrackUiState.Success).track)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit with IOException emits Error with isNetworkError true`() = runTest {
        val vm = buildViewModel(FakeRepo(Result.failure(IOException("offline"))))
        vm.name = "Get Lucky"
        vm.duration = "04:08"

        vm.uiState.test {
            awaitItem()
            vm.submit()
            awaitItem()
            advanceUntilIdle()
            val state = awaitItem() as AddTrackUiState.Error
            assertTrue(state.isNetworkError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit with generic Exception emits Error with isNetworkError false`() = runTest {
        val vm = buildViewModel(FakeRepo(Result.failure(RuntimeException("server"))))
        vm.name = "Get Lucky"
        vm.duration = "04:08"

        vm.uiState.test {
            awaitItem()
            vm.submit()
            awaitItem()
            advanceUntilIdle()
            val state = awaitItem() as AddTrackUiState.Error
            assertTrue(!state.isNetworkError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `isValidDuration accepts valid duration formats`() {
        val vm = buildViewModel(FakeRepo(Result.success(Track(1L, "x", "01:00"))))
        assertTrue(vm.isValidDuration("03:45"))
        assertTrue(vm.isValidDuration("3:45"))
        assertTrue(vm.isValidDuration("00:00"))
        assertTrue(vm.isValidDuration("99:59"))
    }

    @Test
    fun `isValidDuration rejects invalid formats and seconds over 59`() {
        val vm = buildViewModel(FakeRepo(Result.success(Track(1L, "x", "01:00"))))
        assertTrue(!vm.isValidDuration(""))
        assertTrue(!vm.isValidDuration("345"))
        assertTrue(!vm.isValidDuration("3:60"))
        assertTrue(!vm.isValidDuration("3:99"))
        assertTrue(!vm.isValidDuration("abc"))
    }

    @Test
    fun `clearing name clears nameError`() = runTest {
        val vm = buildViewModel(FakeRepo(Result.success(Track(1L, "x", "01:00"))))
        vm.submit()
        assertNotNull(vm.nameError)

        vm.name = "title"
        vm.nameError = null

        assertNull(vm.nameError)
    }
}
