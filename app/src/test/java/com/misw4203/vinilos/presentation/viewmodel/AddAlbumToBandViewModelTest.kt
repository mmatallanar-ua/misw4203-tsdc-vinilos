package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.usecase.AddAlbumToBandUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumsUseCase
import com.misw4203.vinilos.domain.usecase.GetBandDetailUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddAlbumToBandViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeBandRepo(
        var bandResult: Result<Band> = Result.success(sampleBand()),
        var addResult: Result<Unit> = Result.success(Unit),
    ) : BandRepository {
        val added = mutableListOf<Pair<Int, Long>>()
        override suspend fun getBands(): List<BandSummary> = emptyList()
        override suspend fun getBandDetail(id: Int): Band = bandResult.getOrThrow()
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = Unit
        override suspend fun addAlbumToBand(bandId: Int, albumId: Long) {
            addResult.getOrThrow()
            added += bandId to albumId
        }
        override suspend fun addPrizeToBand(bandId: Int, prizeId: Int, premiationDate: String) {}
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
        bandRepo: FakeBandRepo = FakeBandRepo(),
        albumRepo: FakeAlbumRepo = FakeAlbumRepo(),
        bandId: Int = 1,
    ) = AddAlbumToBandViewModel(
        getAlbums = GetAlbumsUseCase(albumRepo),
        getBandDetail = GetBandDetailUseCase(bandRepo),
        addAlbumToBand = AddAlbumToBandUseCase(bandRepo),
        savedStateHandle = SavedStateHandle(mapOf(Destinations.AddAlbumBandArg to bandId)),
    )

    @Test
    fun `starts Loading then Ready excluding albums already on the band`() = runTest {
        val vm = buildViewModel()

        vm.uiState.test {
            assertEquals(AddAlbumToBandUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddAlbumToBandUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // Band already has album id=1; catalog has 1 and 2 → only id=2 available
        val form = vm.form.value
        assertEquals(1, form.filteredAvailable.size)
        assertEquals(2L, form.filteredAvailable[0].id)
        assertTrue(1L in form.currentAlbumIds)
    }

    @Test
    fun `onAddAlbum success calls repository and moves album to current`() = runTest {
        val bandRepo = FakeBandRepo()
        val vm = buildViewModel(bandRepo = bandRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onAddAlbum(2L)
            assertEquals(AddAlbumToBandUiState.Adding(2L), vm.uiState.value)
            advanceUntilIdle()
            assertEquals(AddAlbumToBandEvent.AddedSuccessfully("Siembra"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(listOf(1 to 2L), bandRepo.added)
        assertTrue(2L in vm.form.value.currentAlbumIds)
        assertFalse(vm.form.value.filteredAvailable.any { it.id == 2L })
    }

    @Test
    fun `onAddAlbum network failure emits Error and AddFailed`() = runTest {
        val bandRepo = FakeBandRepo(addResult = Result.failure(IOException("offline")))
        val vm = buildViewModel(bandRepo = bandRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onAddAlbum(2L)
            advanceUntilIdle()
            assertEquals(AddAlbumToBandEvent.AddFailed(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(
            AddAlbumToBandUiState.Error(isNetworkError = true, albumId = 2L),
            vm.uiState.value,
        )
    }

    @Test
    fun `emits network Error when band detail load fails`() = runTest {
        val vm = buildViewModel(bandRepo = FakeBandRepo(bandResult = Result.failure(IOException("offline"))))

        vm.uiState.test {
            assertEquals(AddAlbumToBandUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(
                AddAlbumToBandUiState.Error(isNetworkError = true, albumId = null),
                awaitItem(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sampleBand() = Band(
    id = 1,
    name = "Fania All-Stars",
    image = "",
    description = "",
    creationDate = "1968-01-01T00:00:00.000Z",
    members = emptyList(),
    albums = listOf(Album(1L, "Live at the Cheetah", "", "Fania All-Stars", "1971", "Salsa")),
)

private fun sampleAlbums() = listOf(
    Album(1L, "Live at the Cheetah", "", "Fania All-Stars", "1971", "Salsa"),
    Album(2L, "Siembra", "", "Rubén Blades", "1978", "Salsa"),
)
