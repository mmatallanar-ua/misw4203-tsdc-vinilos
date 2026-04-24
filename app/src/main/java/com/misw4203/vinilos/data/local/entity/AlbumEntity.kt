package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.Album

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val coverUrl: String,
    val artistName: String,
    val releaseYear: String,
    val genre: String,
) {
    fun toDomain() = Album(
        id = id,
        name = name,
        coverUrl = coverUrl,
        artistName = artistName,
        releaseYear = releaseYear,
        genre = genre,
    )

    companion object {
        fun fromDomain(album: Album) = AlbumEntity(
            id = album.id,
            name = album.name,
            coverUrl = album.coverUrl,
            artistName = album.artistName,
            releaseYear = album.releaseYear,
            genre = album.genre,
        )
    }
}
