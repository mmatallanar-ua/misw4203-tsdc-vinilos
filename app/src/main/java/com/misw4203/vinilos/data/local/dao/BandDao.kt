package com.misw4203.vinilos.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.misw4203.vinilos.data.local.entity.BandDetailEntity
import com.misw4203.vinilos.data.local.entity.BandListEntity

@Dao
interface BandDao {

    @Query("SELECT * FROM bands ORDER BY name ASC")
    suspend fun getAll(): List<BandListEntity>

    @Upsert
    suspend fun upsertAll(bands: List<BandListEntity>)

    @Query("DELETE FROM bands")
    suspend fun clear()

    @Transaction
    suspend fun replaceBands(bands: List<BandListEntity>) {
        clear()
        upsertAll(bands)
    }

    @Query("SELECT * FROM band_details WHERE id = :id")
    suspend fun getDetailById(id: Int): BandDetailEntity?

    @Upsert
    suspend fun upsertDetail(detail: BandDetailEntity)
}
