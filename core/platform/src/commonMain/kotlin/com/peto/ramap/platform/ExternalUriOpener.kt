package com.peto.ramap.platform

expect object ExternalUriOpener {
    fun open(uri: String)

    fun isSupportedWebUri(uri: String): Boolean

    val isAppleMapsAvailable: Boolean

    fun openAppleMaps(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
    )
}
