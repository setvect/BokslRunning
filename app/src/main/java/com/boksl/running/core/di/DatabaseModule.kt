package com.boksl.running.core.di

import android.content.Context
import androidx.room.Room
import com.boksl.running.data.local.db.AppDatabase
import com.boksl.running.data.local.db.dao.RunningSessionDao
import com.boksl.running.data.local.db.dao.TrackPointDao
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
}
