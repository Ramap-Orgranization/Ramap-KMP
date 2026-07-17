package com.peto.ramap.platform.permission

import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.darwin.NSObject

internal class IosLocationPermissionDelegate(
    private val onResult: (PermissionStatus) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val status = manager.authorizationStatus
        onResult(permissionStatus(status))
    }

    override fun locationManager(
        manager: CLLocationManager,
        didChangeAuthorizationStatus: CLAuthorizationStatus,
    ) {
        onResult(permissionStatus(didChangeAuthorizationStatus))
    }

    private fun permissionStatus(status: CLAuthorizationStatus): PermissionStatus =
        when (status) {
            kCLAuthorizationStatusAuthorizedWhenInUse,
            kCLAuthorizationStatusAuthorizedAlways,
            -> PermissionStatus.Granted

            kCLAuthorizationStatusDenied,
            kCLAuthorizationStatusRestricted,
            -> PermissionStatus.Blocked

            else -> PermissionStatus.Denied
        }
}
