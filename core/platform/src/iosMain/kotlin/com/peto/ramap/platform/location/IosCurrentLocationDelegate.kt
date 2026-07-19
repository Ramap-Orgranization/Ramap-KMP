package com.peto.ramap.platform.location

import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject

internal class IosCurrentLocationDelegate(
    private val onLocation: (CLLocation?) -> Unit,
) : NSObject(),
    CLLocationManagerDelegateProtocol {
    override fun locationManager(
        manager: CLLocationManager,
        didUpdateLocations: List<*>,
    ) {
        onLocation(didUpdateLocations.lastOrNull() as? CLLocation)
    }

    override fun locationManager(
        manager: CLLocationManager,
        didFailWithError: NSError,
    ) {
        onLocation(null)
    }
}
