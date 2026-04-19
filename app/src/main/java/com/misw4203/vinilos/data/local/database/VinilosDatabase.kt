package com.misw4203.vinilos.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.misw4203.vinilos.data.local.converter.Converters
import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.dao.CollectorDao
import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.entity.AlbumDetailEntity
import com.misw4203.vinilos.data.local.entity.AlbumEntity
import com.misw4203.vinilos.data.local.entity.CollectorDetailEntity
import com.misw4203.vinilos.data.local.entity.CollectorEntity
import com.misw4203.vinilos.data.local.entity.MusicianDetailEntity
import com.misw4203.vinilos.data.local.entity.MusicianListEntity

@Database(
    entities = [
        AlbumEntity::class,
        AlbumDetailEntity::class,
        MusicianListEntity::class,
        MusicianDetailEntity::class,
        CollectorEntity::class,
        CollectorDetailEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class VinilosDatabase : RoomDatabase() {
    abstract fun albumDao(): AlbumDao
    abstract fun musicianDao(): MusicianDao
    abstract fun collectorDao(): CollectorDao

    companion object {
        const val DATABASE_NAME = "vinilos.db"
    }
}
