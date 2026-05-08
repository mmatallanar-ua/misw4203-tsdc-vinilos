package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.CollectorDto
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
