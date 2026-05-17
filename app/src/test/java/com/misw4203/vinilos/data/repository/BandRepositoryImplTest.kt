package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.BandDao
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.BandDetailDto
import com.misw4203.vinilos.data.remote.dto.BandDto
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDto
import com.misw4203.vinilos.data.remote.dto.PrizeInAssociationDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class BandRepositoryImplTest {

    private val api: VinilosApiService = mockk()
    private val dao: BandDao = mockk(relaxed = true)
    private val repo = BandRepositoryImpl(api, dao)

    @Test
    fun `getBands network success caches and returns mapped list`() = runTest {
        coEvery { api.getBands() } returns listOf(
            BandDto(1, "Queen", "img1", "desc", "1970-01-01"),
            BandDto(2, "Aerosmith", "img2", "desc", "1970-01-01"),
        )

        val result = repo.getBands()

        assertEquals(2, result.size)
        assertEquals("Queen", result[0].name)
        coVerify { dao.replaceBands(any()) }
    }

    @Test
    fun `getBands IOException returns cache when populated`() = runTest {
        coEvery { api.getBands() } throws IOException("offline")
        coEvery { dao.getAll() } returns listOf(BandListEntity(1, "Queen", ""))

        val result = repo.getBands()

        assertEquals(1, result.size)
        assertEquals("Queen", result[0].name)
    }

    @Test
    fun `getBands IOException rethrows when cache empty`() = runTest {
        coEvery { api.getBands() } throws IOException("offline")
        coEvery { dao.getAll() } returns emptyList()

        try {
            repo.getBands()
            fail("Expected IOException")
        } catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun `getBands HttpException propagates`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        coEvery { api.getBands() } throws httpError

        try {
            repo.getBands()
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test
    fun `getBandDetail success upserts and returns mapped band`() = runTest {
        coEvery { api.getBandDetail(1) } returns BandDetailDto(
            id = 1,
            name = "Queen",
            image = "img",
            description = "desc",
            creationDate = "1970-01-01",
            musicians = listOf(
                MusicianDetailDto(10, "Freddie", "img-f", "", "1946-09-05", emptyList(), emptyList())
            ),
            albums = listOf(
                AlbumDto(100L, "A Night at the Opera", "cover", "1975-01-01", null, "Rock", null, null, null, null)
            ),
            performerPrizes = listOf(PerformerPrizeDto(id = 10, premiationDate = "1985-01-01")),
        )
        coEvery { api.getPerformerPrizes() } returns listOf(
            PerformerPrizeDetailDto(
                id = 10,
                premiationDate = "1985-01-01",
                prize = PrizeInAssociationDto(
                    id = 10,
                    name = "Grammy",
                    description = "Best Rock",
                    organization = "Recording Academy",
                ),
            ),
        )

        val result = repo.getBandDetail(1)

        assertEquals("Queen", result.name)
        assertEquals(1, result.members.size)
        assertEquals("Freddie", result.members[0].name)
        assertEquals(1, result.prizes.size)
        assertEquals("Grammy", result.prizes[0].name)
        coVerify { dao.upsertDetail(any()) }
    }

    @Test
    fun `getBandDetail IOException returns cache when available`() = runTest {
        coEvery { api.getBandDetail(1) } throws IOException("offline")
        coEvery { api.getPerformerPrizes() } returns emptyList()
        coEvery { dao.getDetailById(1) } returns BandDetailEntity(
            id = 1, name = "Queen", image = "", description = "", creationDate = "",
            members = emptyList(), albums = emptyList(),
        )

        val result = repo.getBandDetail(1)
        assertEquals("Queen", result.name)
    }

    @Test
    fun `getBandDetail IOException rethrows when no cache`() = runTest {
        coEvery { api.getBandDetail(1) } throws IOException("offline")
        coEvery { api.getPerformerPrizes() } returns emptyList()
        coEvery { dao.getDetailById(1) } returns null

        try {
            repo.getBandDetail(1)
            fail("Expected IOException")
        } catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun `addMusicianToBand success write-through updates cached detail`() = runTest {
        val cachedBand = BandDetailEntity(
            id = 1, name = "Queen", image = "", description = "", creationDate = "",
            members = emptyList(), albums = emptyList(),
        )
        coEvery { dao.getDetailById(1) } returns cachedBand
        coEvery { api.addMusicianToBand(1, 10) } returns Unit
        coEvery { api.getMusicianDetail(10) } returns MusicianDetailDto(
            10, "Freddie", "img", "", "1946-09-05", emptyList(), emptyList()
        )

        repo.addMusicianToBand(1, 10)

        coVerify {
            dao.upsertDetail(match { it.members.size == 1 && it.members[0].name == "Freddie" })
        }
    }

    @Test
    fun `addMusicianToBand without cache only posts and skips local update`() = runTest {
        coEvery { dao.getDetailById(1) } returns null
        coEvery { api.addMusicianToBand(1, 10) } returns Unit

        repo.addMusicianToBand(1, 10)

        coVerify(exactly = 0) { dao.upsertDetail(any()) }
    }

    @Test
    fun `addMusicianToBand IOException rethrows leaving cache intact`() = runTest {
        coEvery { api.addMusicianToBand(1, 10) } throws IOException("offline")

        var threw = false
        try {
            repo.addMusicianToBand(1, 10)
        } catch (e: IOException) {
            threw = true
        }
        assertTrue("Expected IOException", threw)
        coVerify(exactly = 0) { dao.upsertDetail(any()) }
    }

    @Test
    fun `addMusicianToBand HttpException propagates and skips cache update`() = runTest {
        val httpError = HttpException(
            Response.error<Any>(409, "".toResponseBody("text/plain".toMediaType()))
        )
        coEvery { api.addMusicianToBand(1, 10) } throws httpError

        var threw = false
        try {
            repo.addMusicianToBand(1, 10)
        } catch (e: HttpException) {
            threw = true
            assertEquals(409, e.code())
        }
        assertTrue("Expected HttpException", threw)
        coVerify(exactly = 0) { dao.upsertDetail(any()) }
    }
}
