package com.boksl.running.data.location

import com.boksl.running.domain.model.LocationSample
import kotlinx.coroutines.flow.Flow

data class LocationRequestConfig(
    val intervalMillis: Long,
    val minUpdateIntervalMillis: Long,
)

interface LocationClient {
    fun observeLocationUpdates(config: LocationRequestConfig): Flow<LocationSample>

    suspend fun getCurrentLocation(): LocationSample?
}
