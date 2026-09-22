package ryo.myappcompany.fixingfieldsynclogger.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepository
import ryo.myappcompany.fixingfieldsynclogger.repository.FieldSyncLoggerRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FieldSyncLoggerModule {

    @Singleton
    @Binds
    abstract fun bindFieldSyncLoggerRepository(
        fieldSyncLoggerRepositoryImpl: FieldSyncLoggerRepositoryImpl
    ): FieldSyncLoggerRepository
}
