package com.boksl.running.ui.feature.permission

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPermissionSupportTest {
    @Test
    fun returnsGrantedWhenPermissionExists() {
        assertEquals(
            LocationPermissionUiState.Granted,
            resolveLocationPermissionUiState(
                hasPermission = true,
                shouldShowRationale = false,
                rationaleAlreadyShown = false,
            ),
        )
    }

    @Test
    fun returnsDeniedWhenRationaleShouldBeShown() {
        assertEquals(
            LocationPermissionUiState.Denied,
            resolveLocationPermissionUiState(
                hasPermission = false,
                shouldShowRationale = true,
                rationaleAlreadyShown = false,
            ),
        )
    }

    @Test
    fun returnsPermanentlyDeniedWhenPreviouslyShownWithoutRationale() {
        assertEquals(
            LocationPermissionUiState.PermanentlyDenied,
            resolveLocationPermissionUiState(
                hasPermission = false,
                shouldShowRationale = false,
                rationaleAlreadyShown = true,
            ),
        )
    }
}
