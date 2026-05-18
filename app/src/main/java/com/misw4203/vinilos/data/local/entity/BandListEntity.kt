package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.BandSummary

@Entity(
    tableName = "bands",
    indices = [Index(value = ["name"])],
)
data class BandListEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val image: String,
) {
    fun toDomain() = BandSummary(
        id = id,
        name = name,
        image = image,
    )

    companion object {
        fun fromDomain(summary: BandSummary) = BandListEntity(
            id = summary.id,
            name = summary.name,
            image = summary.image,
        )
    }
}
