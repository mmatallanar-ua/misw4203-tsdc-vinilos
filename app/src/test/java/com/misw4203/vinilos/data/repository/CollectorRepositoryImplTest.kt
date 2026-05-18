package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.PerformerKind
import com.misw4203.vinilos.testsupport.RecordingAppLogger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CollectorRepositoryImplTest {

    private lateinit var api: VinilosApiService
    private lateinit var dao: CollectorDao
    private lateinit var repository: CollectorRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = CollectorRepositoryImpl(api, dao, UnconfinedTestDispatcher(), RecordingAppLogger())
    }

    // -- getCollectors (HU05) ------------------------------------------------

    @Test
    fun `getCollectors returns mapped summaries from API and replaces cache`() = runTest {
        coEvery { api.getCollectors() } returns listOf(
            collectorDto(id = 100, name = "Manolo Bellon", telephone = "3502457896", email = "manollo@caracol.com.co"),
            collectorDto(id = 101, name = "Jaime Monsalve", telephone = "3102178976", email = "j.monsalve@rtvc.com.co"),
        )

        val result = repository.getCollectors()

        assertEquals(2, result.size)
        assertEquals("Manolo Bellon", result[0].name)
        assertEquals("3502457896", result[0].telephone)
        assertEquals("manollo@caracol.com.co", result[0].email)
        assertEquals(101, result[1].id)
        coVerify(exactly = 1) { dao.replaceCollectors(any()) }
    }

    @Test
    fun `getCollectors falls back to cache when API throws IOException`() = runTest {
        coEvery { api.getCollectors() } throws IOException("offline")
        coEvery { dao.getAll() } returns listOf(
            CollectorEntity(id = 100, name = "Manolo Bellon", telephone = "3502457896", email = "manollo@caracol.com.co"),
        )

        val result = repository.getCollectors()

        assertEquals(1, result.size)
        assertEquals("Manolo Bellon", result[0].name)
        coVerify(exactly = 1) { dao.getAll() }
    }

    @Test(expected = IOException::class)
    fun `getCollectors propagates IOException when cache is empty`() = runTest {
        coEvery { api.getCollectors() } throws IOException("offline")
        coEvery { dao.getAll() } returns emptyList()

        repository.getCollectors()
    }

    @Test(expected = HttpException::class)
    fun `getCollectors propagates HttpException`() = runTest {
        coEvery { api.getCollectors() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        repository.getCollectors()
    }

    // -- removal operations (Fase 1) -----------------------------------------

    @Test
    fun `removeFavoriteMusician delegates to API and prunes cache`() = runTest {
        coEvery { api.removeMusicianFromCollector(100, 7) } returns Unit
        coEvery { dao.getDetailById(100) } returns detailEntity(
            performers = listOf(
                Performer(7L, "Músico", "", PerformerKind.MUSICIAN),
                Performer(9L, "Otro", "", PerformerKind.MUSICIAN),
            ),
        )
        val captured = slot<CollectorDetailEntity>()
        coEvery { dao.upsertDetail(capture(captured)) } returns Unit

        repository.removeFavoriteMusician(100, 7)

        coVerify(exactly = 1) { api.removeMusicianFromCollector(100, 7) }
        assertEquals(listOf(9L), captured.captured.favoritePerformers.map { it.id })
    }

    @Test
    fun `removeFavoriteBand delegates to API`() = runTest {
        coEvery { api.removeBandFromCollector(100, 3) } returns Unit
        coEvery { dao.getDetailById(any()) } returns null

        repository.removeFavoriteBand(100, 3)

        coVerify(exactly = 1) { api.removeBandFromCollector(100, 3) }
    }

    @Test
    fun `removeAlbumFromCollector delegates to API and prunes cached album`() = runTest {
        coEvery { api.removeAlbumFromCollector(100, 8) } returns Unit
        coEvery { dao.getDetailById(100) } returns detailEntity(
            albums = listOf(
                CollectorAlbum(1, 10.0, "Active", Album(5L, "Keep", "", "", "", "")),
                CollectorAlbum(2, 20.0, "Active", Album(8L, "Drop", "", "", "", "")),
            ),
        )
        val captured = slot<CollectorDetailEntity>()
        coEvery { dao.upsertDetail(capture(captured)) } returns Unit

        repository.removeAlbumFromCollector(100, 8)

        coVerify(exactly = 1) { api.removeAlbumFromCollector(100, 8) }
        assertEquals(listOf(5L), captured.captured.collectorAlbums.mapNotNull { it.album?.id })
    }

    private fun detailEntity(
        performers: List<Performer> = emptyList(),
        albums: List<CollectorAlbum> = emptyList(),
    ) = CollectorDetailEntity(
        id = 100,
        name = "Manolo",
        telephone = "300",
        email = "m@test.com",
        description = "",
        collectorAlbums = albums,
        favoritePerformers = performers,
        comments = emptyList(),
    )

    private fun collectorDto(
        id: Int,
        name: String = "name",
        telephone: String = "0000000000",
        email: String = "email@test.com",
    ) = CollectorDto(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
        description = null,
        comments = emptyList(),
        collectorAlbums = emptyList(),
        favoritePerformers = emptyList(),
    )
}
