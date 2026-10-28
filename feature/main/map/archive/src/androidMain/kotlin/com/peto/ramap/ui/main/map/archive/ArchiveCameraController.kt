package com.peto.ramap.ui.main.map.archive

import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapInteractionConfig

internal class ArchiveCameraController {
    private var lastFocusKey = ""

    fun focusShops(
        map: NaverMap,
        shops: RamenShops,
    ): Boolean {
        if (shops.isEmpty()) return false
        val key = shops.values.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" }
        if (key == lastFocusKey) return true
        lastFocusKey = key
        map.locationTrackingMode = LocationTrackingMode.None
        if (shops.size == 1) {
            val shop = shops.values.first()
            map.moveCamera(
                CameraUpdate.scrollAndZoomTo(
                    LatLng(shop.location.lat, shop.location.lng),
                    ArchiveMapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toDouble(),
                ),
            )
        } else {
            val bounds = LatLngBounds.Builder()
            shops.values.forEach { bounds.include(LatLng(it.location.lat, it.location.lng)) }
            map.moveCamera(CameraUpdate.fitBounds(bounds.build(), FOCUS_PADDING_PX))
        }
        return true
    }

    private companion object {
        const val FOCUS_PADDING_PX = 120
    }
}
