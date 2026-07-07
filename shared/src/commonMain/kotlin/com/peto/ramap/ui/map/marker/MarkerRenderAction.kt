package com.peto.ramap.ui.map.marker

internal interface MarkerRenderAction {
    fun removeMarkers(keys: Set<String>)

    fun addMarkers(entries: List<MarkerRenderEntry>)
}
