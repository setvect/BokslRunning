package com.boksl.running.core.di

import android.content.Context
import androidx.room.Room
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.data.debug.DebugRunSeedGenerator
import com.boksl.running.data.debug.DebugSeedManager
import com.boksl.running.data.location.DefaultLocationClient
import com.boksl.running.data.location.LocationClient
import com.boksl.running.data.repository.DefaultProfileRepository
import com.boksl.running.data.repository.DefaultRunEngineRepository
import com.boksl.running.data.repository.DefaultRunningRepository
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.domain.repository.RunningRepository
import com.boksl.running.ui.feature.run.DefaultRunTrackingServiceController
import com.boksl.running.ui.feature.run.RunTrackingServiceController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "bokslrunning.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRunningSessionDao(database: AppDatabase): RunningSessionDao = database.runningSessionDao()

    @Provides
    fun provideTrackPointDao(database: AppDatabase): TrackPointDao = database.trackPointDao()

    @Provides
    @Singleton
    fun provideRunningRepository(
        runningSessionDao: RunningSessionDao,
        trackPointDao: TrackPointDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        clock: Clock,
    ): RunningRepository =
        DefaultRunningRepository(
            runningSessionDao = runningSessionDao,
            trackPointDao = trackPointDao,
            ioDispatcher = ioDispatcher,
            clock = clock,
        )

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideLocationClient(defaultLocationClient: DefaultLocationClient): LocationClient = defaultLocationClient

    @Provides
    @Singleton
    fun provideRunEngineRepository(defaultRunEngineRepository: DefaultRunEngineRepository): RunEngineRepository =
        defaultRunEngineRepository

    @Provides
    @Singleton
    fun provideRunTrackingServiceController(
        defaultRunTrackingServiceController: DefaultRunTrackingServiceController,
    ): RunTrackingServiceController = defaultRunTrackingServiceController

    @Provides
    @Singleton
    fun provideProfileRepository(profilePreferencesDataSource: ProfilePreferencesDataSource): ProfileRepository =
        DefaultProfileRepository(profilePreferencesDataSource)

    @Provides
    @Singleton
    fun provideDebugSeedManager(debugRunSeedGenerator: DebugRunSeedGenerator): DebugSeedManager = debugRunSeedGenerator
}
