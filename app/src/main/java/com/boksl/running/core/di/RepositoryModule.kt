package com.boksl.running.core.di

import com.boksl.running.data.debug.DebugRunSeedGenerator
import com.boksl.running.data.debug.DebugSeedManager
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.data.repository.DefaultExportRepository
import com.boksl.running.data.repository.DefaultImportRepository
import com.boksl.running.data.repository.DefaultProfileRepository
import com.boksl.running.data.repository.DefaultRunningRepository
import com.boksl.running.domain.repository.ExportRepository
import com.boksl.running.domain.repository.ImportRepository
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideRunningRepository(defaultRunningRepository: DefaultRunningRepository): RunningRepository =
        defaultRunningRepository

    @Provides
    @Singleton
    fun provideProfileRepository(profilePreferencesDataSource: ProfilePreferencesDataSource): ProfileRepository =
        DefaultProfileRepository(profilePreferencesDataSource)

    @Provides
    @Singleton
    fun provideExportRepository(defaultExportRepository: DefaultExportRepository): ExportRepository =
        defaultExportRepository

    @Provides
    @Singleton
    fun provideImportRepository(defaultImportRepository: DefaultImportRepository): ImportRepository =
        defaultImportRepository

    @Provides
    @Singleton
    fun provideDebugSeedManager(debugRunSeedGenerator: DebugRunSeedGenerator): DebugSeedManager = debugRunSeedGenerator
}
