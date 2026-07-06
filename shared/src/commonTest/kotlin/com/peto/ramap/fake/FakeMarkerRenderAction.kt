package com.peto.ramap.fake

import com.peto.ramap.domain.model.Marker
import com.peto.ramap.ui.map.marker.MarkerRenderAction
import com.peto.ramap.ui.map.marker.MarkerRenderEntry

class FakeMarkerRenderAction : MarkerRenderAction {
    val boundMarkers = mutableListOf<Map<String, Marker>>()
    val removedKeys = mutableListOf<Set<String>>()
    val addedEntries = mutableListOf<List<MarkerRenderEntry>>()

    override fun bindMarkers(markers: Map<String, Marker>) {
        boundMarkers += markers
    }

    override fun removeMarkers(keys: Set<String>) {
        removedKeys += keys
    }

    override fun addMarkers(entries: List<MarkerRenderEntry>) {
        addedEntries += entries
    }
}
