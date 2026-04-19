package com.misw4203.vinilos.di

import android.content.Context
import androidx.room.Room
import com.misw4203.vinilos.data.local.dao.AlbumDao
import com.misw4203.vinilos.data.local.dao.MusicianDao
import com.misw4203.vinilos.data.local.database.VinilosDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVinilosDatabase(
        @ApplicationContext context: Context,
    ): VinilosDatabase = Room.databaseBuilder(
        context,
        VinilosDatabase::class.java,
        VinilosDatabase.DATABASE_NAME,
    )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideAlbumDao(database: VinilosDatabase): AlbumDao = database.albumDao()

    @Provides
    fun provideMusicianDao(database: VinilosDatabase): MusicianDao = database.musicianDao()
}
