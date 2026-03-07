package com.boksl.running.ui.feature.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat

val locationPermissions =
    arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

sealed interface LocationPermissionUiState {
    data object Granted : LocationPermissionUiState

    data object Denied : LocationPermissionUiState

    data object PermanentlyDenied : LocationPermissionUiState

    data object ShowRequest : LocationPermissionUiState
}

enum class PermissionReturnAction {
    None,
    CompleteOnboarding,
    StartRun,
}

fun hasLocationPermission(context: Context): Boolean =
    locationPermissions.any { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

fun shouldShowLocationPermissionRationale(activity: Activity): Boolean =
    locationPermissions.any { permission ->
        androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

fun resolveLocationPermissionUiState(
    hasPermission: Boolean,
    shouldShowRationale: Boolean,
    rationaleAlreadyShown: Boolean,
): LocationPermissionUiState =
    when {
        hasPermission -> LocationPermissionUiState.Granted
        shouldShowRationale -> LocationPermissionUiState.Denied
        rationaleAlreadyShown -> LocationPermissionUiState.PermanentlyDenied
        else -> LocationPermissionUiState.ShowRequest
    }

fun Context.findActivity(): ComponentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is ComponentActivity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

fun openAppSettings(context: Context) {
    val intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        )
    context.startActivity(intent)
}
