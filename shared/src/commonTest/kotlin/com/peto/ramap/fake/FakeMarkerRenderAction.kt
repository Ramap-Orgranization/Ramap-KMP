package com.peto.ramap.fake

import com.peto.ramap.ui.map.marker.MarkerRenderAction
import com.peto.ramap.ui.map.marker.MarkerRenderEntry

class FakeMarkerRenderAction : MarkerRenderAction {
    val removedKeys = mutableListOf<Set<String>>()
    val addedEntries = mutableListOf<List<MarkerRenderEntry>>()

    override fun removeMarkers(keys: Set<String>) {
        removedKeys += keys
    }

    override fun addMarkers(entries: List<MarkerRenderEntry>) {
        addedEntries += entries
    }
}
