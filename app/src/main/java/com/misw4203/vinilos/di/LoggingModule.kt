package com.misw4203.vinilos.di

import com.misw4203.vinilos.core.logging.AndroidAppLogger
import com.misw4203.vinilos.core.logging.AppLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {
    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: AndroidAppLogger): AppLogger
}
