package com.misw4203.vinilos.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.misw4203.vinilos.MainDispatcherRule
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.BandSummary
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.model.MusicianSummary
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.PerformerKind
import com.misw4203.vinilos.domain.model.Track
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.usecase.AddBandToAlbumUseCase
import com.misw4203.vinilos.domain.usecase.AddMusicianToAlbumUseCase
import com.misw4203.vinilos.domain.usecase.GetAlbumDetailUseCase
import com.misw4203.vinilos.domain.usecase.GetBandsUseCase
import com.misw4203.vinilos.domain.usecase.GetMusiciansUseCase
import com.misw4203.vinilos.presentation.navigation.Destinations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AddPerformerToAlbumViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakeAlbumRepo(
        var detailResult: Result<AlbumDetail> = Result.success(sampleAlbum()),
        var addError: Throwable? = null,
    ) : AlbumRepository {
        val addedMusicians = mutableListOf<Pair<Long, Int>>()
        val addedBands = mutableListOf<Pair<Long, Int>>()
        override suspend fun getAlbums(): List<Album> = emptyList()
        override suspend fun getAlbumById(id: Long): AlbumDetail = detailResult.getOrThrow()
        override suspend fun addTrack(albumId: Long, request: com.misw4203.vinilos.data.remote.dto.CreateTrackRequest): Track = error("unused")
        override suspend fun addComment(albumId: Long, description: String, rating: Int, collectorId: Int): Comment = error("unused")
        override suspend fun createAlbum(input: CreateAlbumInput): Album = error("unused")
        override suspend fun addMusicianToAlbum(albumId: Long, musicianId: Int) {
            addError?.let { throw it }
            addedMusicians += albumId to musicianId
        }
        override suspend fun addBandToAlbum(albumId: Long, bandId: Int) {
            addError?.let { throw it }
            addedBands += albumId to bandId
        }
    }

    private class FakeMusicianRepo : MusicianRepository {
        var all: List<MusicianSummary> = emptyList()
        override suspend fun getMusicians(): List<MusicianSummary> = all
        override suspend fun getMusicianDetail(id: Int) = error("unused")
        override suspend fun addAlbumToMusician(musicianId: Int, albumId: Int) = Unit
        override suspend fun addPrizeToMusician(musicianId: Int, prizeId: Int, premiationDate: String) = Unit
    }

    private class FakeBandRepo : BandRepository {
        var all: List<BandSummary> = emptyList()
        override suspend fun getBands(): List<BandSummary> = all
        override suspend fun getBandDetail(id: Int) = error("unused")
        override suspend fun addMusicianToBand(bandId: Int, musicianId: Int) = Unit
    }

    private fun build(
        albumRepo: FakeAlbumRepo = FakeAlbumRepo(),
        musicianRepo: FakeMusicianRepo = FakeMusicianRepo().apply {
            all = listOf(MusicianSummary(10, "Rubén Blades", "", ""), MusicianSummary(11, "Willie Colón", "", ""))
        },
        bandRepo: FakeBandRepo = FakeBandRepo().apply {
            all = listOf(BandSummary(20, "Fania All-Stars", ""))
        },
        albumId: Long = 1L,
    ) = AddPerformerToAlbumViewModel(
        getMusicians = GetMusiciansUseCase(musicianRepo),
        getBands = GetBandsUseCase(bandRepo),
        getAlbumDetail = GetAlbumDetailUseCase(albumRepo),
        addMusicianToAlbum = AddMusicianToAlbumUseCase(albumRepo),
        addBandToAlbum = AddBandToAlbumUseCase(albumRepo),
        savedStateHandle = SavedStateHandle(mapOf(Destinations.AddPerformerAlbumArg to albumId)),
    )

    @Test
    fun `loads and excludes performers already on the album`() = runTest {
        val vm = build()
        vm.uiState.test {
            assertEquals(AddPerformerToAlbumUiState.Loading, awaitItem())
            advanceUntilIdle()
            assertEquals(AddPerformerToAlbumUiState.Ready, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        // Album already has performer id=10 (a musician) → excluded from musicians
        val form = vm.form.value
        assertTrue(form.filteredMusicians.none { it.id == 10 })
        assertEquals(1, form.filteredMusicians.size)
        assertEquals(11, form.filteredMusicians[0].id)
    }

    @Test
    fun `onAddMusician calls album repo and moves to current`() = runTest {
        val albumRepo = FakeAlbumRepo()
        val vm = build(albumRepo = albumRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onAddMusician(11)
            advanceUntilIdle()
            assertEquals(AddPerformerToAlbumEvent.AddedSuccessfully("Willie Colón"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(listOf(1L to 11), albumRepo.addedMusicians)
        assertTrue(11 in vm.form.value.currentMusicianIds)
    }

    @Test
    fun `onAddBand calls album repo`() = runTest {
        val albumRepo = FakeAlbumRepo()
        val vm = build(albumRepo = albumRepo)
        advanceUntilIdle()

        vm.onTabChange(PerformerType.BAND)
        vm.onAddBand(20)
        advanceUntilIdle()

        assertEquals(listOf(1L to 20), albumRepo.addedBands)
    }

    @Test
    fun `add failure emits AddFailed`() = runTest {
        val albumRepo = FakeAlbumRepo(addError = IOException("offline"))
        val vm = build(albumRepo = albumRepo)
        advanceUntilIdle()

        vm.events.test {
            vm.onAddMusician(11)
            advanceUntilIdle()
            assertEquals(AddPerformerToAlbumEvent.AddFailed(isNetworkError = true), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun sampleAlbum() = AlbumDetail(
    id = 1L,
    name = "Siembra",
    coverUrl = "",
    artistName = "Rubén Blades",
    releaseDate = "1978",
    genre = "Salsa",
    recordLabel = "Fania Records",
    description = "",
    tracks = emptyList(),
    performers = listOf(Performer(10L, "Rubén Blades", "", PerformerKind.MUSICIAN)),
    comments = emptyList(),
)
