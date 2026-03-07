package com.boksl.running.ui.feature.run

import android.content.Context
import android.content.Intent
import androidx.lifecycle.LifecycleService
import com.boksl.running.data.location.LocationClient
import com.boksl.running.data.location.LocationRequestConfig
import com.boksl.running.data.repository.DefaultRunEngineRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class RunTrackingService : LifecycleService() {
    @Inject
    lateinit var runEngineRepository: DefaultRunEngineRepository

    @Inject
    lateinit var locationClient: LocationClient

    @Inject
    lateinit var notificationManager: TrackingNotificationManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var collectionJobActive = false

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startTracking() {
        if (collectionJobActive) return
        startForeground(
            TrackingNotificationManager.NOTIFICATION_ID,
            notificationManager.createNotification(),
        )
        collectionJobActive = true
        locationClient
            .observeLocationUpdates(
                LocationRequestConfig(
                    intervalMillis = 5_000L,
                    minUpdateIntervalMillis = 2_000L,
                ),
            ).onEach { sample ->
                runEngineRepository.onLocationSample(sample)
            }.catch {
                stopTracking()
            }.launchIn(serviceScope)
    }

    private fun stopTracking() {
        collectionJobActive = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val ACTION_START = "com.boksl.running.action.START_TRACKING"
        private const val ACTION_STOP = "com.boksl.running.action.STOP_TRACKING"

        fun createStartIntent(context: Context): Intent = Intent(context, RunTrackingService::class.java).setAction(ACTION_START)

        fun createStopIntent(context: Context): Intent = Intent(context, RunTrackingService::class.java).setAction(ACTION_STOP)
    }
}
