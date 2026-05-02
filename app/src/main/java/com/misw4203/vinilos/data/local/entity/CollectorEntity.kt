package com.misw4203.vinilos.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.misw4203.vinilos.domain.model.CollectorSummary

@Entity(tableName = "collectors")
data class CollectorEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val telephone: String,
    val email: String,
) {
    fun toDomain() = CollectorSummary(
        id = id,
        name = name,
        telephone = telephone,
        email = email,
    )

    companion object {
        fun fromDomain(summary: CollectorSummary) = CollectorEntity(
            id = summary.id,
            name = summary.name,
            telephone = summary.telephone,
            email = summary.email,
        )
    }
}
