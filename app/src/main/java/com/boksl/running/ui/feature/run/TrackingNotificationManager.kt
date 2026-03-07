package com.boksl.running.ui.feature.run

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackingNotificationManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun createNotification(): Notification {
            ensureChannel()
            return NotificationCompat
                .Builder(context, CHANNEL_ID)
                .setContentTitle("복슬달리기 기록 중")
                .setContentText("러닝 기록을 계속 수집하고 있어요.")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build()
        }

        private fun ensureChannel() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    "러닝 기록",
                    NotificationManager.IMPORTANCE_LOW,
                )
            manager.createNotificationChannel(channel)
        }

        companion object {
            const val CHANNEL_ID = "run_tracking_channel"
            const val NOTIFICATION_ID = 1001
        }
    }
