package com.peto.ramap.ui.map.marker

import com.peto.ramap.domain.model.Marker

internal class MarkerRenderKeyPolicy {
    fun key(marker: Marker): String =
        when (marker) {
            is Marker.SingleMarker -> "$SINGLE_MARKER_PREFIX$MARKER_KEY_SEPARATOR${marker.id}"
            is Marker.ClusterMaker -> "$CLUSTER_MARKER_PREFIX$MARKER_KEY_SEPARATOR${marker.id}"
        }

    companion object {
        private const val SINGLE_MARKER_PREFIX = "shop"
        private const val CLUSTER_MARKER_PREFIX = "cluster"
        private const val MARKER_KEY_SEPARATOR = ":"
    }
}
