package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.MusicianDetailDto
import com.misw4203.vinilos.data.remote.dto.PerformerPrizeDto
import com.misw4203.vinilos.data.remote.dto.PrizeDetailDto
import io.mockk.coEvery
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
    private lateinit var repository: MusicianRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        repository = MusicianRepositoryImpl(api)
    }

    @Test
    fun `getMusicianDetail on success fetches prizes and returns musician`() = runTest {
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
    }

    @Test(expected = IOException::class)
    fun `getMusicianDetail re-throws IOException`() = runTest {
        coEvery { api.getMusicianDetail(2) } throws IOException("offline")

        repository.getMusicianDetail(2)
    }

    @Test(expected = HttpException::class)
    fun `getMusicianDetail re-throws HttpException`() = runTest {
        coEvery { api.getMusicianDetail(2) } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        repository.getMusicianDetail(2)
    }

    @Test
    fun `getMusicians on success returns mapped summaries`() = runTest {
        coEvery { api.getMusicians() } returns listOf(
            musicianDto(id = 100, name = "Rubén Blades", image = "url1"),
            musicianDto(id = 101, name = "Queen", image = "url2"),
        )

        val result = repository.getMusicians()

        assertEquals(2, result.size)
        assertEquals("Rubén Blades", result[0].name)
        assertEquals("url2", result[1].image)
    }

    @Test(expected = IOException::class)
    fun `getMusicians re-throws IOException`() = runTest {
        coEvery { api.getMusicians() } throws IOException("offline")

        repository.getMusicians()
    }

    @Test(expected = HttpException::class)
    fun `getMusicians re-throws HttpException`() = runTest {
        coEvery { api.getMusicians() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        repository.getMusicians()
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
