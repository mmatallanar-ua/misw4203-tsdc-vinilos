package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.CollectorComment
import com.misw4203.vinilos.domain.model.CollectorDetail
import com.misw4203.vinilos.domain.model.Performer

@Entity(tableName = "collector_details")
data class CollectorDetailEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val telephone: String,
    val email: String,
    val description: String,
    val collectorAlbums: List<CollectorAlbum>,
    val favoritePerformers: List<Performer>,
    val comments: List<CollectorComment>,
) {
    fun toDomain() = CollectorDetail(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
        description = description,
        collectorAlbums = collectorAlbums,
        favoritePerformers = favoritePerformers,
        comments = comments,
    )

    companion object {
        fun fromDomain(d: CollectorDetail) = CollectorDetailEntity(
            id = d.id,
            name = d.name,
            telephone = d.telephone,
            email = d.email,
            description = d.description,
            collectorAlbums = d.collectorAlbums,
            favoritePerformers = d.favoritePerformers,
            comments = d.comments,
        )
    }
}
