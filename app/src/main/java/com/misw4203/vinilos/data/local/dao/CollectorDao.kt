package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity

@Dao
interface CollectorDao {

    @Query("SELECT * FROM collectors")
    suspend fun getCollectors(): List<CollectorEntity>

    @Upsert
    suspend fun upsertCollectors(collectors: List<CollectorEntity>)

    @Query("DELETE FROM collectors")
    suspend fun clearCollectors()

    @Transaction
    suspend fun replaceCollectors(collectors: List<CollectorEntity>) {
        clearCollectors()
        upsertCollectors(collectors)
    }

    @Query("SELECT * FROM collector_detail WHERE id = :id")
    suspend fun getCollectorDetail(id: Int): CollectorDetailEntity?

    @Upsert
    suspend fun upsertCollectorDetail(detail: CollectorDetailEntity)
}
