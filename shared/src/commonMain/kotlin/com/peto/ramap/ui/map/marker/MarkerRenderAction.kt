package com.peto.ramap.ui.map.marker

import com.peto.ramap.domain.model.Marker

internal interface MarkerRenderAction {
    fun bindMarkers(markers: Map<String, Marker>)

    fun removeMarkers(keys: Set<String>)

    fun addMarkers(entries: List<MarkerRenderEntry>)
}
