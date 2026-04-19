package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.AlbumDetail
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.Performer
import com.misw4203.vinilos.domain.model.Track

@Entity(tableName = "album_detail")
data class AlbumDetailEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val releaseDate: String,
    val genre: String,
    val recordLabel: String,
    val description: String,
    val tracks: List<Track>,
    val performers: List<Performer>,
    val comments: List<Comment>,
) {
    fun toDomain() = AlbumDetail(
        id = id,
        name = name,
        coverUrl = coverUrl,
        artistName = artistName,
        releaseDate = releaseDate,
        genre = genre,
        recordLabel = recordLabel,
        description = description,
        tracks = tracks,
        performers = performers,
        comments = comments,
    )

    companion object {
        fun fromDomain(detail: AlbumDetail) = AlbumDetailEntity(
            id = detail.id,
            name = detail.name,
            coverUrl = detail.coverUrl,
            artistName = detail.artistName,
            releaseDate = detail.releaseDate,
            genre = detail.genre,
            recordLabel = detail.recordLabel,
            description = detail.description,
            tracks = detail.tracks,
            performers = detail.performers,
            comments = detail.comments,
        )
    }
}
