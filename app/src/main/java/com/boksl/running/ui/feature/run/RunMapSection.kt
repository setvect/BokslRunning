package com.boksl.running.ui.feature.run

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boksl.running.BuildConfig
import com.boksl.running.domain.model.TrackPoint
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

@Composable
fun runMapSection(
    currentLocation: LatLng?,
    routePoints: List<LatLng>,
    modifier: Modifier = Modifier,
    maxZoom: Float = DEFAULT_MAX_ZOOM,
    minZoom: Float = DEFAULT_MIN_ZOOM,
) {
    Card(modifier = modifier.height(260.dp)) {
        if (BuildConfig.MAPS_API_KEY.isBlank()) {
            mapFallback(message = "MAPS_API_KEY가 없어서 지도를 표시할 수 없습니다.")
            return@Card
        }

        val cameraPositionState = rememberCameraPositionState()
        val bounds = routePoints.toBoundsOrNull()
        val context = LocalContext.current

        LaunchedEffect(bounds, currentLocation) {
            moveCamera(
                cameraPositionState = cameraPositionState,
                bounds = bounds,
                currentLocation = currentLocation,
                maxZoom = maxZoom,
                minZoom = minZoom,
            )
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties =
                MapProperties(
                    isBuildingEnabled = true,
                    isIndoorEnabled = true,
                    isMyLocationEnabled = false,
                    maxZoomPreference = maxZoom,
                    minZoomPreference = minZoom,
                ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false),
        ) {
            currentLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = if (context.resources.configuration.locales[0].language == Locale.KOREAN.language) "현재 위치" else "Current location",
                )
            }
            if (routePoints.size >= 2) {
                Polyline(
                    points = routePoints,
                    color = MaterialTheme.colorScheme.primary,
                    width = 12f,
                )
            }
        }
    }
}

fun TrackPoint.toLatLng(): LatLng = LatLng(latitude, longitude)

private suspend fun moveCamera(
    cameraPositionState: CameraPositionState,
    bounds: LatLngBounds?,
    currentLocation: LatLng?,
    maxZoom: Float,
    minZoom: Float,
) {
    when {
        bounds != null -> {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, MAP_PADDING_PX))
            clampZoom(cameraPositionState, minZoom = minZoom, maxZoom = maxZoom)
        }
        currentLocation != null -> {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(currentLocation, DEFAULT_LOCATION_ZOOM))
            clampZoom(cameraPositionState, minZoom = minZoom, maxZoom = maxZoom)
        }
    }
}

private suspend fun clampZoom(
    cameraPositionState: CameraPositionState,
    minZoom: Float,
    maxZoom: Float,
) {
    val currentZoom = cameraPositionState.position.zoom
    val clampedZoom = currentZoom.coerceIn(minZoom, maxZoom)
    if (currentZoom != clampedZoom) {
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(
                    cameraPositionState.position.target,
                    clampedZoom,
                ),
            ),
        )
    }
}

private fun List<LatLng>.toBoundsOrNull(): LatLngBounds? {
    if (isEmpty()) return null
    val builder = LatLngBounds.builder()
    forEach(builder::include)
    return builder.build()
}

private const val DEFAULT_LOCATION_ZOOM = 16f
private const val DEFAULT_MAX_ZOOM = 18f
private const val DEFAULT_MIN_ZOOM = 12f
private const val MAP_PADDING_PX = 96

@Composable
private fun mapFallback(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
