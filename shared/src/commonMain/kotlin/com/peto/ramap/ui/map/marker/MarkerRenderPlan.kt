package com.peto.ramap.ui.map.marker

import com.peto.ramap.domain.model.Marker

data class MarkerRenderPlan(
    val currentMarkers: Map<String, Marker>,
    val removeKeys: Set<String>,
    val addEntries: List<MarkerRenderEntry>,
)
