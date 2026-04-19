package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.Collector
import com.misw4203.vinilos.domain.model.CollectorAlbum
import com.misw4203.vinilos.domain.model.Comment
import com.misw4203.vinilos.domain.model.FavoritePerformer

@Entity(tableName = "collector_detail")
data class CollectorDetailEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val telephone: String,
    val email: String,
    val comments: List<Comment>,
    val favoritePerformers: List<FavoritePerformer>,
    val collectorAlbums: List<CollectorAlbum>,
) {
    fun toDomain() = Collector(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
        comments = comments,
        favoritePerformers = favoritePerformers,
        collectorAlbums = collectorAlbums,
    )

    companion object {
        fun fromDomain(collector: Collector) = CollectorDetailEntity(
            id = collector.id,
            name = collector.name,
            telephone = collector.telephone,
            email = collector.email,
            comments = collector.comments,
            favoritePerformers = collector.favoritePerformers,
            collectorAlbums = collector.collectorAlbums,
        )
    }
}
