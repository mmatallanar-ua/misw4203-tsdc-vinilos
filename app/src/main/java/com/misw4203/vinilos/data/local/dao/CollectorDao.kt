package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity

@Dao
interface CollectorDao {

    @Query("SELECT * FROM collectors ORDER BY name ASC")
    suspend fun getAll(): List<CollectorEntity>

    @Upsert
    suspend fun upsertAll(collectors: List<CollectorEntity>)

    @Query("DELETE FROM collectors")
    suspend fun clear()

    @Transaction
    suspend fun replaceCollectors(collectors: List<CollectorEntity>) {
        clear()
        upsertAll(collectors)
    }

    @Query("SELECT * FROM collector_details WHERE id = :id")
    suspend fun getDetailById(id: Int): CollectorDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: CollectorDetailEntity)
}
