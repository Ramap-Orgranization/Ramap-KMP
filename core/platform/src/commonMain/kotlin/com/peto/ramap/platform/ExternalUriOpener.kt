package com.peto.ramap.platform

expect object ExternalUriOpener {
    fun open(uri: String)

    fun isSupportedWebUri(uri: String): Boolean
}
