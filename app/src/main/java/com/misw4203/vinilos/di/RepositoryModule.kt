package com.misw4203.vinilos.di

import com.misw4203.vinilos.data.repository.AlbumRepositoryImpl
import com.misw4203.vinilos.data.repository.BandRepositoryImpl
import com.misw4203.vinilos.data.repository.CollectorRepositoryImpl
import com.misw4203.vinilos.data.repository.MusicianRepositoryImpl
import com.misw4203.vinilos.data.repository.PrizeRepositoryImpl
import com.misw4203.vinilos.domain.repository.AlbumRepository
import com.misw4203.vinilos.domain.repository.BandRepository
import com.misw4203.vinilos.domain.repository.CollectorRepository
import com.misw4203.vinilos.domain.repository.MusicianRepository
import com.misw4203.vinilos.domain.repository.PrizeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicianRepository(impl: MusicianRepositoryImpl): MusicianRepository

    @Binds
    @Singleton
    abstract fun bindAlbumRepository(impl: AlbumRepositoryImpl): AlbumRepository

    @Binds
    @Singleton
    abstract fun bindCollectorRepository(impl: CollectorRepositoryImpl): CollectorRepository

    @Binds
    @Singleton
    abstract fun bindBandRepository(impl: BandRepositoryImpl): BandRepository

    @Binds
    @Singleton
    abstract fun bindPrizeRepository(impl: PrizeRepositoryImpl): PrizeRepository
}
