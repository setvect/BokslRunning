package com.boksl.running.core.di

import com.boksl.running.data.location.DefaultLocationClient
import com.boksl.running.data.location.LocationClient
import com.boksl.running.data.repository.DefaultRunEngineRepository
import com.boksl.running.domain.repository.RunEngineRepository
import com.boksl.running.ui.feature.run.DefaultRunTrackingServiceController
import com.boksl.running.ui.feature.run.RunTrackingServiceController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RuntimeModule {
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
}
