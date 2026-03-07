package com.boksl.running.ui.feature.run

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RunTrackingServiceController {
    fun startTracking()

    fun stopTracking()
}

@Singleton
class DefaultRunTrackingServiceController
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : RunTrackingServiceController {
        override fun startTracking() {
            ContextCompat.startForegroundService(context, RunTrackingService.createStartIntent(context))
        }

        override fun stopTracking() {
            context.startService(RunTrackingService.createStopIntent(context))
        }
    }
