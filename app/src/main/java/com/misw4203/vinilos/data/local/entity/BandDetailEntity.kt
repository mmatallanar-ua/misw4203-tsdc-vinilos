package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.Album
import com.misw4203.vinilos.domain.model.Band
import com.misw4203.vinilos.domain.model.MusicianPrize
import com.misw4203.vinilos.domain.model.MusicianSummary

@Entity(tableName = "band_details")
data class BandDetailEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
    val description: String,
    val creationDate: String,
    val members: List<MusicianSummary>,
    val albums: List<Album>,
    val prizes: List<MusicianPrize> = emptyList(),
) {
    fun toDomain() = Band(
        id = id,
        name = name,
        image = image,
        description = description,
        creationDate = creationDate,
        members = members,
        albums = albums,
        prizes = prizes,
    )

    companion object {
        fun fromDomain(band: Band) = BandDetailEntity(
            id = band.id,
            name = band.name,
            image = band.image,
            description = band.description,
            creationDate = band.creationDate,
            members = band.members,
            albums = band.albums,
            prizes = band.prizes,
        )
    }
}
