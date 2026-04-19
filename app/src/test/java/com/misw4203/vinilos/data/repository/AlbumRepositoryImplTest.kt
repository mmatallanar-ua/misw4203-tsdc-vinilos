package com.misw4203.vinilos.data.repository

import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity
import com.misw4203.vinilos.data.remote.api.VinilosApiService
import com.misw4203.vinilos.data.remote.dto.AlbumDto
import com.misw4203.vinilos.data.remote.dto.CommentDto
import com.misw4203.vinilos.data.remote.dto.PerformerDto
import com.misw4203.vinilos.data.remote.dto.TrackDto
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track
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
    fun `getAlbums on success returns mapped remote and replaces cache`() = runTest {
        coEvery { api.getAlbums() } returns listOf(
            albumDto(id = 1L, name = "Buscando América", genre = "Salsa"),
            albumDto(id = 2L, name = "A Night at the Opera", genre = "Rock"),
        )

        val result = repository.getAlbums()

        assertEquals(2, result.size)
        assertEquals("Buscando América", result[0].name)
        assertEquals("Salsa", result[0].genre)
        coVerify(exactly = 1) {
            dao.replaceAlbums(match { list ->
                list.size == 2 && list[0].id == 1L && list[1].id == 2L
            })
        }
    }

    @Test
    fun `getAlbums falls back to cache on IOException`() = runTest {
        coEvery { api.getAlbums() } throws IOException("offline")
        coEvery { dao.getAlbums() } returns listOf(
            AlbumEntity(
                id = 9L,
                name = "Cached Album",
                coverUrl = "",
                artistName = "Cached Artist",
                releaseYear = "2000",
                genre = "Jazz",
            )
        )

        val result = repository.getAlbums()

        assertEquals(1, result.size)
        assertEquals("Cached Album", result[0].name)
        coVerify(exactly = 0) { dao.replaceAlbums(any()) }
    }

    @Test(expected = IOException::class)
    fun `getAlbums re-throws IOException when cache is empty`() = runTest {
        coEvery { api.getAlbums() } throws IOException("offline")
        coEvery { dao.getAlbums() } returns emptyList()

        repository.getAlbums()
    }

    @Test(expected = HttpException::class)
    fun `getAlbums re-throws HttpException without touching cache read`() = runTest {
        coEvery { api.getAlbums() } throws HttpException(
            Response.error<Any>(500, "".toResponseBody("text/plain".toMediaType()))
        )

        try {
            repository.getAlbums()
        } finally {
            coVerify(exactly = 0) { dao.getAlbums() }
        }
    }

    @Test
    fun `getAlbumById on success returns mapped remote and upserts detail cache`() = runTest {
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
        coVerify(exactly = 1) { dao.upsertAlbumDetail(match { it.id == 42L }) }
    }

    @Test
    fun `getAlbumById falls back to cached detail on IOException`() = runTest {
        val cached = AlbumDetailEntity(
            id = 5L,
            name = "Cached Detail",
            coverUrl = "",
            artistName = "X",
            releaseDate = "2001",
            genre = "Pop",
            recordLabel = "Label",
            description = "Desc",
            tracks = listOf(Track(1L, "t", "1:00")),
            performers = listOf(Performer(1L, "X", "")),
            comments = listOf(Comment(1L, "ok", 4)),
        )
        coEvery { api.getAlbum(5L) } throws IOException("offline")
        coEvery { dao.getAlbumDetail(5L) } returns cached

        val result = repository.getAlbumById(5L)

        assertEquals("Cached Detail", result.name)
        assertEquals(1, result.tracks.size)
    }

    @Test(expected = IOException::class)
    fun `getAlbumById re-throws IOException when cached detail is absent`() = runTest {
        coEvery { api.getAlbum(7L) } throws IOException("offline")
        coEvery { dao.getAlbumDetail(7L) } returns null

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
