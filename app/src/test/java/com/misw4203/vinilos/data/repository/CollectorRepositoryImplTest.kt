package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CollectorAlbumDto
import com.misw4203.vinilos.data.remote.dto.CollectorDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.FavoritePerformerDto
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.FavoritePerformer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class CollectorRepositoryImplTest {

    private lateinit var api: VinilosApiService
    private lateinit var dao: CollectorDao
    private lateinit var repository: CollectorRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = CollectorRepositoryImpl(api, dao)
    }

    @Test
    fun `getCollectors on success returns mapped summaries and replaces cache`() = runTest {
        coEvery { api.getCollectors() } returns listOf(
            collectorDto(id = 100, name = "Manolo", email = "m@a.co", telephone = "3500"),
            collectorDto(id = 101, name = "Jaime", email = "j@a.co", telephone = "3012"),
        )

        val result = repository.getCollectors()

        assertEquals(2, result.size)
        assertEquals("Manolo", result[0].name)
        assertEquals("j@a.co", result[1].email)
        coVerify(exactly = 1) {
            dao.replaceCollectors(match { it.size == 2 && it[0].id == 100 })
        }
    }

    @Test
    fun `getCollectors falls back to cache on IOException`() = runTest {
        coEvery { api.getCollectors() } throws IOException("offline")
        coEvery { dao.getCollectors() } returns listOf(
            CollectorEntity(id = 7, name = "Cached", email = "c@a.co", telephone = "555"),
        )

        val result = repository.getCollectors()

        assertEquals(1, result.size)
        assertEquals("Cached", result[0].name)
        coVerify(exactly = 0) { dao.replaceCollectors(any()) }
    }

    @Test(expected = IOException::class)
    fun `getCollectors re-throws IOException when cache is empty`() = runTest {
        coEvery { api.getCollectors() } throws IOException("offline")
        coEvery { dao.getCollectors() } returns emptyList()

        repository.getCollectors()
    }

    @Test(expected = HttpException::class)
    fun `getCollectors re-throws HttpException without reading cache`() = runTest {
        coEvery { api.getCollectors() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        try {
            repository.getCollectors()
        } finally {
            coVerify(exactly = 0) { dao.getCollectors() }
        }
    }

    @Test
    fun `getCollectorById on success returns mapped remote and upserts detail cache`() = runTest {
        coEvery { api.getCollector(100) } returns collectorDto(
            id = 100,
            name = "Manolo",
            email = "m@a.co",
            telephone = "3500",
            comments = listOf(CommentDto(1L, "great", 5)),
            favoritePerformers = listOf(FavoritePerformerDto(1, "Blades", "img", "desc", null, null)),
            collectorAlbums = listOf(CollectorAlbumDto(1L, 35, "Active")),
        )

        val result = repository.getCollectorById(100)

        assertEquals("Manolo", result.name)
        assertEquals(1, result.comments.size)
        assertEquals(5, result.comments[0].rating)
        assertEquals(35, result.collectorAlbums[0].price)
        assertEquals("Active", result.collectorAlbums[0].status)
        coVerify(exactly = 1) { dao.upsertCollectorDetail(match { it.id == 100 }) }
    }

    @Test
    fun `getCollectorById falls back to cached detail on IOException`() = runTest {
        val cached = CollectorDetailEntity(
            id = 5,
            name = "Cached",
            telephone = "555",
            email = "c@a.co",
            comments = listOf(Comment(1L, "d", 4)),
            favoritePerformers = listOf(FavoritePerformer(1, "p", "", "")),
            collectorAlbums = listOf(CollectorAlbum(1L, 10, "Active")),
        )
        coEvery { api.getCollector(5) } throws IOException("offline")
        coEvery { dao.getCollectorDetail(5) } returns cached

        val result = repository.getCollectorById(5)

        assertEquals("Cached", result.name)
        assertEquals(1, result.favoritePerformers.size)
    }

    @Test(expected = IOException::class)
    fun `getCollectorById re-throws IOException when cached detail is absent`() = runTest {
        coEvery { api.getCollector(9) } throws IOException("offline")
        coEvery { dao.getCollectorDetail(9) } returns null

        repository.getCollectorById(9)
    }

    @Test(expected = HttpException::class)
    fun `getCollectorById re-throws HttpException on 404`() = runTest {
        coEvery { api.getCollector(1) } throws HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )

        repository.getCollectorById(1)
    }

    private fun collectorDto(
        id: Int,
        name: String = "n",
        email: String? = "e@e.co",
        telephone: String? = "000",
        comments: List<CommentDto>? = emptyList(),
        favoritePerformers: List<FavoritePerformerDto>? = emptyList(),
        collectorAlbums: List<CollectorAlbumDto>? = emptyList(),
    ) = CollectorDto(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
        comments = comments,
        favoritePerformers = favoritePerformers,
        collectorAlbums = collectorAlbums,
    )
}
