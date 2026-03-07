package com.boksl.running.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.boksl.running.domain.model.LocationSample
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DefaultLocationClient
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : LocationClient {
        private val fusedLocationProviderClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        @SuppressLint("MissingPermission")
        override fun observeLocationUpdates(config: LocationRequestConfig): Flow<LocationSample> =
            callbackFlow {
                val request =
                    LocationRequest
                        .Builder(Priority.PRIORITY_HIGH_ACCURACY, config.intervalMillis)
                        .setMinUpdateIntervalMillis(config.minUpdateIntervalMillis)
                        .build()

                val callback =
                    object : LocationCallback() {
                        override fun onLocationResult(result: LocationResult) {
                            result.locations.forEach { location ->
                                trySend(location.toSample())
                            }
                        }
                    }

                fusedLocationProviderClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                awaitClose {
                    fusedLocationProviderClient.removeLocationUpdates(callback)
                }
            }

        @SuppressLint("MissingPermission")
        override suspend fun getCurrentLocation(): LocationSample? =
            suspendCancellableCoroutine { continuation ->
                val request =
                    CurrentLocationRequest
                        .Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build()

                fusedLocationProviderClient
                    .getCurrentLocation(request, null)
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location?.toSample())
                        }
                    }.addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            }
    }

private fun Location.toSample(): LocationSample =
    LocationSample(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = accuracy,
        speedMps = speed,
        altitudeMeters = altitude,
        recordedAtEpochMillis = time,
    )
