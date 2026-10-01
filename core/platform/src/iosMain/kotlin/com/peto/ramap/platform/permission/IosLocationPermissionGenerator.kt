package com.peto.ramap.platform.permission

import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted

internal class IosLocationPermissionGenerator(
    private val locationManager: CLLocationManager,
    private val onResult: (PermissionStatus) -> Unit,
) : LocationPermissionGenerator {
    private var isPermissionRequestPending = false
    private val delegate =
        IosLocationPermissionDelegate { status ->
            when (status) {
                PermissionStatus.Granted -> {
                    isPermissionRequestPending = false
                    onResult(status)
                }

                PermissionStatus.Blocked -> {
                    if (isPermissionRequestPending) {
                        isPermissionRequestPending = false
                        onResult(status)
                    }
                }

                PermissionStatus.Denied -> Unit
            }
        }

    init {
        locationManager.delegate = delegate
    }

    override fun hasPermission(): Boolean {
        val status = locationManager.authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }

    override fun requestPermission() {
        when (locationManager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> {
                isPermissionRequestPending = false
                onResult(PermissionStatus.Granted)
            }

            kCLAuthorizationStatusNotDetermined -> {
                isPermissionRequestPending = true
                locationManager.requestWhenInUseAuthorization()
            }

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> {
                isPermissionRequestPending = false
                onResult(PermissionStatus.Blocked)
            }

            else -> onResult(PermissionStatus.Denied)
        }
    }
}
