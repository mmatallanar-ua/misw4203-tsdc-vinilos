package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Musician
import com.misw4203.vinilos.domain.model.MusicianPrize

@Entity(tableName = "musician_details")
data class MusicianDetailEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val birthDate: String,
    val albums: List<Album>,
    val prizes: List<MusicianPrize>,
) {
    fun toDomain() = Musician(
        id = id,
        name = name,
        image = image,
        description = description,
        birthDate = birthDate,
        albums = albums,
        prizes = prizes,
    )

    companion object {
        fun fromDomain(musician: Musician) = MusicianDetailEntity(
            id = musician.id,
            name = musician.name,
            image = musician.image,
            description = musician.description,
            birthDate = musician.birthDate,
            albums = musician.albums,
            prizes = musician.prizes,
        )
    }
}
