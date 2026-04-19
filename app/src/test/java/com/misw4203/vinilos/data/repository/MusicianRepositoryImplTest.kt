package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDto
import com.misw4203.vinilos.data.remote.dto.PrizeDetailDto
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.MusicianPrize
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

class MusicianRepositoryImplTest {

    private lateinit var api: VinilosApiService
    private lateinit var dao: MusicianDao
    private lateinit var repository: MusicianRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = MusicianRepositoryImpl(api, dao)
    }

    @Test
    fun `getMusicianDetail on success fetches prizes and upserts cache`() = runTest {
        coEvery { api.getMusicianDetail(2) } returns MusicianDetailDto(
            id = 2,
            name = "Rubén Blades",
            image = "img.jpg",
            description = "desc",
            birthDate = "1948-07-16",
            albums = emptyList(),
            performerPrizes = listOf(PerformerPrizeDto(id = 10, premiationDate = "1985-01-01")),
        )
        coEvery { api.getPrizeDetail(10) } returns PrizeDetailDto(
            id = 10,
            organization = "Recording Academy",
            name = "Grammy",
            description = "Best Latin Album",
            performerPrizes = emptyList(),
        )

        val result = repository.getMusicianDetail(2)

        assertEquals("Rubén Blades", result.name)
        assertEquals(1, result.prizes.size)
        assertEquals("Grammy", result.prizes[0].name)
        assertEquals("1985-01-01", result.prizes[0].premiationDate)
        coVerify(exactly = 1) { dao.upsertMusicianDetail(match { it.id == 2 }) }
    }

    @Test
    fun `getMusicianDetail falls back to cache on IOException`() = runTest {
        val cached = MusicianDetailEntity(
            id = 2,
            name = "Cached Musician",
            image = "",
            description = "",
            birthDate = "1948-07-16",
            albums = listOf(Album(1L, "Album", "", "", "1984", "Salsa")),
            prizes = listOf(MusicianPrize(99, "P", "O", "D", "2000-01-01")),
        )
        coEvery { api.getMusicianDetail(2) } throws IOException("offline")
        coEvery { dao.getMusicianDetail(2) } returns cached

        val result = repository.getMusicianDetail(2)

        assertEquals("Cached Musician", result.name)
        assertEquals(1, result.prizes.size)
        assertEquals(1, result.albums.size)
    }

    @Test(expected = IOException::class)
    fun `getMusicianDetail re-throws IOException when cache is null`() = runTest {
        coEvery { api.getMusicianDetail(2) } throws IOException("offline")
        coEvery { dao.getMusicianDetail(2) } returns null

        repository.getMusicianDetail(2)
    }

    @Test(expected = HttpException::class)
    fun `getMusicianDetail re-throws HttpException without reading cache`() = runTest {
        coEvery { api.getMusicianDetail(2) } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        try {
            repository.getMusicianDetail(2)
        } finally {
            coVerify(exactly = 0) { dao.getMusicianDetail(any()) }
        }
    }

    @Test
    fun `getMusicians on success returns mapped summaries and replaces cache`() = runTest {
        coEvery { api.getMusicians() } returns listOf(
            musicianDto(id = 100, name = "Rubén Blades", image = "url1"),
            musicianDto(id = 101, name = "Queen", image = "url2"),
        )

        val result = repository.getMusicians()

        assertEquals(2, result.size)
        assertEquals("Rubén Blades", result[0].name)
        assertEquals("url2", result[1].image)
        coVerify(exactly = 1) {
            dao.replaceMusicians(match { it.size == 2 && it[0].id == 100 })
        }
    }

    @Test
    fun `getMusicians falls back to cache on IOException`() = runTest {
        coEvery { api.getMusicians() } throws IOException("offline")
        coEvery { dao.getMusicians() } returns listOf(
            MusicianListEntity(id = 7, name = "Cached", image = "i", birthDate = ""),
        )

        val result = repository.getMusicians()

        assertEquals(1, result.size)
        assertEquals("Cached", result[0].name)
        coVerify(exactly = 0) { dao.replaceMusicians(any()) }
    }

    @Test(expected = IOException::class)
    fun `getMusicians re-throws IOException when cache is empty`() = runTest {
        coEvery { api.getMusicians() } throws IOException("offline")
        coEvery { dao.getMusicians() } returns emptyList()

        repository.getMusicians()
    }

    @Test(expected = HttpException::class)
    fun `getMusicians re-throws HttpException without reading cache`() = runTest {
        coEvery { api.getMusicians() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        try {
            repository.getMusicians()
        } finally {
            coVerify(exactly = 0) { dao.getMusicians() }
        }
    }

    private fun musicianDto(
        id: Int,
        name: String = "name",
        image: String = "img",
    ) = MusicianDetailDto(
        id = id,
        name = name,
        image = image,
        description = "",
        birthDate = "",
        albums = emptyList(),
        performerPrizes = emptyList(),
    )
}
