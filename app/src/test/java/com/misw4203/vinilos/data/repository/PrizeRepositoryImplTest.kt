package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CreatePrizeRequest
import com.misw4203.vinilos.data.remote.dto.PrizeDetailDto
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
class PrizeRepositoryImplTest {

    private val api: VinilosApiService = mockk()
    private val repo = PrizeRepositoryImpl(api, UnconfinedTestDispatcher())

    @Test
    fun `getPrizes success maps DTOs to domain`() = runTest {
        coEvery { api.getPrizes() } returns listOf(
            PrizeDetailDto(id = 1, organization = "Recording Academy", name = "Grammy", description = "Best Rock"),
            PrizeDetailDto(id = 2, organization = "MTV", name = "VMA", description = "Best Video"),
        )

        val result = repo.getPrizes()

        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Grammy", result[0].name)
        assertEquals("Best Rock", result[0].description)
        assertEquals("Recording Academy", result[0].organization)
        assertEquals("VMA", result[1].name)
    }

    @Test
    fun `getPrizes empty returns empty list`() = runTest {
        coEvery { api.getPrizes() } returns emptyList()

        assertTrue(repo.getPrizes().isEmpty())
    }

    @Test
    fun `getPrizes IOException propagates`() = runTest {
        coEvery { api.getPrizes() } throws IOException("offline")

        try {
            repo.getPrizes()
            fail("Expected IOException")
        } catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun `getPrizes HttpException propagates`() = runTest {
        coEvery { api.getPrizes() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        try {
            repo.getPrizes()
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(500, e.code())
        }
    }

    @Test
    fun `createPrize sends request and maps response to domain`() = runTest {
        val bodySlot = slot<CreatePrizeRequest>()
        coEvery { api.createPrize(capture(bodySlot)) } returns PrizeDetailDto(
            id = 7, organization = "Recording Academy", name = "Grammy", description = "Best Rock",
        )

        val result = repo.createPrize("Grammy", "Best Rock", "Recording Academy")

        assertEquals(7, result.id)
        assertEquals("Grammy", result.name)
        assertEquals("Best Rock", result.description)
        assertEquals("Recording Academy", result.organization)
        assertEquals("Grammy", bodySlot.captured.name)
        assertEquals("Best Rock", bodySlot.captured.description)
        assertEquals("Recording Academy", bodySlot.captured.organization)
    }

    @Test
    fun `createPrize IOException propagates`() = runTest {
        coEvery { api.createPrize(any()) } throws IOException("offline")

        try {
            repo.createPrize("Grammy", "Best Rock", "Recording Academy")
            fail("Expected IOException")
        } catch (e: IOException) {
            assertTrue(true)
        }
    }

    @Test
    fun `createPrize HttpException propagates`() = runTest {
        coEvery { api.createPrize(any()) } throws HttpException(
            Response.error<Any>(400, "".toResponseBody("text/plain".toMediaType()))
        )

        try {
            repo.createPrize("Grammy", "Best Rock", "Recording Academy")
            fail("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(400, e.code())
        }
    }
}
