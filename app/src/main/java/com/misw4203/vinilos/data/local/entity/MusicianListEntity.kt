package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.MusicianSummary

@Entity(tableName = "musicians")
data class MusicianListEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
    val birthDate: String,
) {
    fun toDomain() = MusicianSummary(
        id = id,
        name = name,
        image = image,
        birthDate = birthDate,
    )

    companion object {
        fun fromDomain(summary: MusicianSummary) = MusicianListEntity(
            id = summary.id,
            name = summary.name,
            image = summary.image,
            birthDate = summary.birthDate,
        )
    }
}
