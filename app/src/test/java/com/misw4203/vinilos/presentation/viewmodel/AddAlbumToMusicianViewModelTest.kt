package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddAlbumToMusicianUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.domain.usecase.GetMusicianDetailUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddAlbumToMusicianViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeMusicianRepo(
        var musicianResult: Result<Musician> = Result.success(sampleMusician()),
        var addResult: Result<Unit> = Result.success(Unit),
    ) : MusicianRepository {
        override suspend fun getMusicians(): List<MusicianSummary> = emptyList()
        override suspend fun getMusicianDetail(id: Int): Musician = musicianResult.getOrThrow()
        override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = addResult.getOrThrow()
        override suspend fun addPrizeToMusician(musicianId: Int, prizeId: Int, premiationDate: String) = Unit
    }

    private class FakeAlbumRepo(
        private val albums: List<Album> = sampleAlbums(),
    ) : AlbumRepository {
        override suspend fun getAlbums(): List<Album> = albums
        override suspend fun getAlbumById(id: Long) = error("not used")
        override suspend fun addTrack(albumId: Long, request: com.misw4203.vinilos.domain.model.NewTrack) = error("not used")
        override suspend fun addComment(albumId: Long, description: String, rating: Int, collectorId: Int) = error("not used")
        override suspend fun createAlbum(input: com.misw4203.vinilos.domain.model.CreateAlbumInput) = error("not used")
        override suspend fun removeTrack(albumId: Long, trackId: Long) {}
        override suspend fun removeComment(albumId: Long, commentId: Long) {}
        override suspend fun addMusicianToAlbum(albumId: Long, musicianId: Int) {}
        override suspend fun addBandToAlbum(albumId: Long, bandId: Int) {}
    }

    private fun buildViewModel(
        musicianRepo: FakeMusicianRepo = FakeMusicianRepo(),
        albumRepo: FakeAlbumRepo = FakeAlbumRepo(),
        musicianId: Int = 100,
    ) = AddAlbumToMusicianViewModel(
        getAlbums = GetAlbumsUseCase(albumRepo),
        getMusicianDetail = GetMusicianDetailUseCase(musicianRepo),
        addAlbumToMusician = AddAlbumToMusicianUseCase(musicianRepo),
        savedStateHandle = SavedStateHandle(mapOf(Destinations.AddAlbumMusicianArg to musicianId)),
    )

    @Test
    fun `starts Loading then emits Ready with available albums excluding current`() = runTest {
        val vm = buildViewModel()

        vm.uiState.test {
            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // Musician has album id=1; catalog has id=1 and id=2 → only id=2 available
        val form = vm.form.value
        assertEquals(1, form.filteredAvailable.size)
        assertEquals(2L, form.filteredAvailable[0].id)
        assertEquals(1, form.currentAlbumIds.size)
        assertTrue(1L in form.currentAlbumIds)
    }

    @Test
    fun `emits network Error when repositories throw IOException`() = runTest {
        val vm = buildViewModel(musicianRepo = FakeMusicianRepo(musicianResult = Result.failure(IOException("offline"))))

        vm.uiState.test {
            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Error(isNetworkError = true, albumId = null), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits server Error when repositories throw HttpException`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        val vm = buildViewModel(musicianRepo = FakeMusicianRepo(musicianResult = Result.failure(httpError)))

        vm.uiState.test {
            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Error(isNetworkError = false, albumId = null), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAddAlbum success moves album from available to current and emits event`() = runTest {
        val vm = buildViewModel()

        vm.uiState.test {
            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Ready, awaitItem())

            vm.onAddAlbum(2)

            assertEquals(AddAlbumToMusicianUiState.Adding(2), awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(2L in vm.form.value.currentAlbumIds)
        assertFalse(vm.form.value.filteredAvailable.any { it.id == 2L })
    }

    @Test
    fun `onAddAlbum network failure emits Error state and AddFailed event`() = runTest {
        val musicianRepo = FakeMusicianRepo(addResult = Result.failure(IOException("offline")))
        val vm = buildViewModel(musicianRepo = musicianRepo)

        var capturedEvent: AddAlbumToMusicianEvent? = null
        vm.uiState.test {
            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Ready, awaitItem())

            vm.events.test {
                vm.onAddAlbum(2)
                // Adding is set synchronously on uiState, not emitted as an event.
                assertEquals(AddAlbumToMusicianUiState.Adding(2), vm.uiState.value)
                advanceUntilIdle()
                capturedEvent = awaitItem()
                cancelAndIgnoreRemainingEvents()
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(capturedEvent is AddAlbumToMusicianEvent.AddFailed)
        assertTrue((capturedEvent as AddAlbumToMusicianEvent.AddFailed).isNetworkError)
    }

    @Test
    fun `onQueryChange filters available albums by name`() = runTest {
        val vm = buildViewModel()
        advanceUntilIdle()

        vm.onQueryChange("opera")

        val filtered = vm.form.value.filteredAvailable
        assertEquals(1, filtered.size)
        assertEquals("A Night at the Opera", filtered[0].name)
    }

    @Test
    fun `retry re-loads data after error`() = runTest {
        val musicianRepo = FakeMusicianRepo(musicianResult = Result.failure(IOException("offline")))
        val vm = buildViewModel(musicianRepo = musicianRepo)

        vm.uiState.test {
            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Error(isNetworkError = true, albumId = null), awaitItem())

            musicianRepo.musicianResult = Result.success(sampleMusician())
            vm.retry()

            assertEquals(AddAlbumToMusicianUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToMusicianUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sampleMusician() = Musician(
    id = 100,
    name = "Rubén Blades",
    image = "",
    description = "Cantante panameño.",
    birthDate = "1948-07-16T00:00:00.000Z",
    albums = listOf(Album(1L, "Buscando América", "", "Rubén Blades", "1984", "Salsa")),
    prizes = emptyList(),
)

private fun sampleAlbums() = listOf(
    Album(1L, "Buscando América", "", "Rubén Blades", "1984", "Salsa"),
    Album(2L, "A Night at the Opera", "", "Queen", "1975", "Rock"),
)
