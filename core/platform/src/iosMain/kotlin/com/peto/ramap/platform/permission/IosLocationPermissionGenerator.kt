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
    private val delegate: IosLocationPermissionDelegate = IosLocationPermissionDelegate(onResult),
) : LocationPermissionGenerator {
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
            -> onResult(PermissionStatus.Granted)

            kCLAuthorizationStatusNotDetermined -> locationManager.requestWhenInUseAuthorization()

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> onResult(PermissionStatus.Blocked)

            else -> onResult(PermissionStatus.Denied)
        }
    }
}
