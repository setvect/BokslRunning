package com.boksl.running.core.di

import android.content.Context
import androidx.room.Room
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
import com.boksl.running.data.local.preferences.ProfilePreferencesDataSource
import com.boksl.running.data.repository.DefaultProfileRepository
import com.boksl.running.data.repository.DefaultRunningRepository
import com.boksl.running.domain.repository.ProfileRepository
import com.boksl.running.domain.repository.RunningRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
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
    ): RunningRepository =
        DefaultRunningRepository(
            runningSessionDao = runningSessionDao,
            trackPointDao = trackPointDao,
            ioDispatcher = ioDispatcher,
        )

    @Provides
    @Singleton
    fun provideProfileRepository(profilePreferencesDataSource: ProfilePreferencesDataSource): ProfileRepository =
        DefaultProfileRepository(profilePreferencesDataSource)
}
