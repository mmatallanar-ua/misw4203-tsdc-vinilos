package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.CreateTrackRequest
import com.misw4203.vinilos.data.remote.dto.PerformerDto
import com.misw4203.vinilos.data.remote.dto.TrackDto
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.CreateAlbumInput
import com.misw4203.vinilos.domain.model.Track
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
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

    // -- createAlbum ---------------------------------------------------------

    @Test
    fun `createAlbum sends request to API and returns mapped album`() = runTest {
        val input = CreateAlbumInput(
            name = "Nuevo Álbum",
            cover = "https://example.com/cover.jpg",
            releaseDate = "2024-01-15",
            description = "Una descripción",
            genre = "Rock",
            recordLabel = "Sony Music",
        )
        coEvery { api.createAlbum(any()) } returns albumDto(
            id = 10L,
            name = "Nuevo Álbum",
            genre = "Rock",
            recordLabel = "Sony Music",
        )

        val result = repository.createAlbum(input)

        assertEquals(10L, result.id)
        assertEquals("Nuevo Álbum", result.name)
        assertEquals("Rock", result.genre)
        coVerify(exactly = 1) { api.createAlbum(any()) }
    }

    @Test(expected = IOException::class)
    fun `createAlbum propagates IOException`() = runTest {
        coEvery { api.createAlbum(any()) } throws IOException("offline")
        repository.createAlbum(
            CreateAlbumInput("n", "c", "2024-01-01", "d", "Rock", "Sony Music")
        )
    }

    @Test(expected = HttpException::class)
    fun `createAlbum propagates HttpException`() = runTest {
        coEvery { api.createAlbum(any()) } throws HttpException(
            Response.error<Any>(422, "".toResponseBody("text/plain".toMediaType()))
        )
        repository.createAlbum(
            CreateAlbumInput("n", "c", "2024-01-01", "d", "Rock", "Sony Music")
        )
    }

    // -- Write-through cache (HU07/HU08/HU09) --------------------------------

    @Test
    fun `createAlbum upserts the new album into the local cache`() = runTest {
        val captured = slot<AlbumEntity>()
        coEvery { api.createAlbum(any()) } returns albumDto(
            id = 99L,
            name = "Nuevo",
            genre = "Rock",
        )
        coEvery { dao.upsert(capture(captured)) } returns Unit

        val result = repository.createAlbum(
            CreateAlbumInput("Nuevo", "c", "2024-01-01", "d", "Rock", "Sony Music")
        )

        assertEquals(99L, result.id)
        coVerify(exactly = 1) { dao.upsert(any()) }
        assertEquals(99L, captured.captured.id)
        assertEquals("Nuevo", captured.captured.name)
    }

    @Test
    fun `addTrack appends the new track to the cached album detail`() = runTest {
        val albumId = 42L
        val cached = AlbumDetailEntity(
            id = albumId,
            name = "OK Computer",
            coverUrl = "",
            artistName = "Radiohead",
            releaseDate = "1997-05-21",
            genre = "Rock",
            recordLabel = "Parlophone",
            description = "",
            tracks = listOf(Track(1L, "Airbag", "4:44")),
            performers = emptyList(),
            comments = emptyList(),
        )
        coEvery { dao.getDetailById(albumId) } returns cached
        coEvery { api.addTrack(albumId, any()) } returns TrackDto(2L, "Karma Police", "4:21")
        val captured = slot<AlbumDetailEntity>()
        coEvery { dao.upsertDetail(capture(captured)) } returns Unit

        val result = repository.addTrack(albumId, CreateTrackRequest("Karma Police", "04:21"))

        assertEquals(2L, result.id)
        coVerify(exactly = 1) { dao.upsertDetail(any()) }
        assertEquals(2, captured.captured.tracks.size)
        assertEquals("Karma Police", captured.captured.tracks[1].name)
    }

    @Test
    fun `addTrack does not touch cache when album detail is not cached`() = runTest {
        val albumId = 50L
        coEvery { dao.getDetailById(albumId) } returns null
        coEvery { api.addTrack(albumId, any()) } returns TrackDto(3L, "X", "01:00")

        repository.addTrack(albumId, CreateTrackRequest("X", "01:00"))

        coVerify(exactly = 0) { dao.upsertDetail(any()) }
    }

    @Test
    fun `addComment appends the new comment to the cached album detail`() = runTest {
        val albumId = 7L
        val cached = AlbumDetailEntity(
            id = albumId,
            name = "Album",
            coverUrl = "",
            artistName = "",
            releaseDate = "",
            genre = "",
            recordLabel = "",
            description = "",
            tracks = emptyList(),
            performers = emptyList(),
            comments = listOf(Comment(10L, "Bueno", 4)),
        )
        coEvery { dao.getDetailById(albumId) } returns cached
        coEvery { api.addComment(albumId, any()) } returns CommentDto(11L, "Excelente", 5)
        val captured = slot<AlbumDetailEntity>()
        coEvery { dao.upsertDetail(capture(captured)) } returns Unit

        repository.addComment(albumId, "Excelente", 5, 100)

        coVerify(exactly = 1) { dao.upsertDetail(any()) }
        assertEquals(2, captured.captured.comments.size)
        assertEquals("Excelente", captured.captured.comments[1].description)
        assertEquals(5, captured.captured.comments[1].rating)
    }

    @Test
    fun `addComment does not touch cache when album detail is not cached`() = runTest {
        val albumId = 8L
        coEvery { dao.getDetailById(albumId) } returns null
        coEvery { api.addComment(albumId, any()) } returns CommentDto(12L, "x", 3)

        repository.addComment(albumId, "x", 3, 100)

        coVerify(exactly = 0) { dao.upsertDetail(any()) }
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
