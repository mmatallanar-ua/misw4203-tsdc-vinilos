package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.data.remote.dto.PerformerDto
import com.misw4203.vinilos.data.remote.dto.TrackDto
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

class AlbumRepositoryImplTest {

    private lateinit var api: VinilosApiService
    private lateinit var dao: AlbumDao
    private lateinit var repository: AlbumRepositoryImpl

    @Before
    fun setUp() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = AlbumRepositoryImpl(api, dao)
    }

    @Test
    fun `getAlbums returns mapped list from API`() = runTest {
        coEvery { api.getAlbums() } returns listOf(
            albumDto(id = 1L, name = "Buscando América", genre = "Salsa"),
            albumDto(id = 2L, name = "A Night at the Opera", genre = "Rock"),
        )

        val result = repository.getAlbums()

        assertEquals(2, result.size)
        assertEquals("Buscando América", result[0].name)
        assertEquals("Salsa", result[0].genre)
        assertEquals(2L, result[1].id)
    }

    @Test(expected = IOException::class)
    fun `getAlbums propagates IOException`() = runTest {
        coEvery { api.getAlbums() } throws IOException("offline")
        repository.getAlbums()
    }

    @Test(expected = HttpException::class)
    fun `getAlbums propagates HttpException`() = runTest {
        coEvery { api.getAlbums() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )
        repository.getAlbums()
    }

    @Test
    fun `getAlbumById returns mapped detail from API`() = runTest {
        coEvery { api.getAlbum(42L) } returns albumDto(
            id = 42L,
            name = "OK Computer",
            releaseDate = "1997-05-21T00:00:00.000Z",
            genre = "Rock",
            recordLabel = "Parlophone",
            description = "Classic album",
            tracks = listOf(TrackDto(1L, "Airbag", "4:44")),
            performers = listOf(PerformerDto(5L, "Radiohead", "img.jpg", null, null, null)),
            comments = listOf(CommentDto(7L, "Great", 5)),
        )

        val result = repository.getAlbumById(42L)

        assertEquals("OK Computer", result.name)
        assertEquals("Radiohead", result.artistName)
        assertEquals(1, result.tracks.size)
        assertEquals("Airbag", result.tracks[0].name)
        assertEquals(1, result.comments.size)
        assertEquals(5, result.comments[0].rating)
    }

    @Test
    fun `addTrack returns mapped Track from API`() = runTest {
        val request = CreateTrackRequest("Get Lucky", "04:08")
        coEvery { api.addTrack(100L, request) } returns TrackDto(1L, "Get Lucky", "04:08")

        val result = repository.addTrack(100L, request)

        assertEquals(1L, result.id)
        assertEquals("Get Lucky", result.name)
        assertEquals("04:08", result.duration)
    }

    @Test(expected = IOException::class)
    fun `addTrack propagates IOException`() = runTest {
        coEvery { api.addTrack(any(), any()) } throws IOException("offline")
        repository.addTrack(100L, CreateTrackRequest("X", "01:00"))
    }

    @Test(expected = HttpException::class)
    fun `addTrack propagates HttpException`() = runTest {
        coEvery { api.addTrack(any(), any()) } throws HttpException(
            Response.error<Any>(404, "".toResponseBody("text/plain".toMediaType()))
        )
        repository.addTrack(999L, CreateTrackRequest("X", "01:00"))
    }

    @Test(expected = IOException::class)
    fun `getAlbumById propagates IOException when cache is empty`() = runTest {
        coEvery { api.getAlbum(7L) } throws IOException("offline")
        coEvery { dao.getDetailById(7L) } returns null
        repository.getAlbumById(7L)
    }

    private fun albumDto(
        id: Long,
        name: String = "name",
        cover: String? = "cover",
        releaseDate: String? = "2000-01-01",
        description: String? = "desc",
        genre: String? = "genre",
        recordLabel: String? = "label",
        performers: List<PerformerDto>? = emptyList(),
        tracks: List<TrackDto>? = emptyList(),
        comments: List<CommentDto>? = emptyList(),
    ) = AlbumDto(
        id = id,
        name = name,
        cover = cover,
        releaseDate = releaseDate,
        description = description,
        genre = genre,
        recordLabel = recordLabel,
        performers = performers,
        tracks = tracks,
        comments = comments,
    )
}
