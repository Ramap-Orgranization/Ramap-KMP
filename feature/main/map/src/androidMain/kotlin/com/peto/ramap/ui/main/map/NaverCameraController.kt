package com.peto.ramap.ui.main.map

import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.LocationTrackingMode
import com.naver.maps.map.NaverMap
import com.peto.ramap.domain.model.shop.Location
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.config.MapInteractionConfig

internal class NaverCameraController {
    private var lastFocusKey = ""
    private var hasFocusedCurrentLocation = false
    private var lastPlaceFocusKey = 0L

    fun focusShops(
        naverMap: NaverMap,
        shops: RamenShops,
        currentLocation: Location?,
        focusRequestKey: Long,
    ): Boolean {
        if (shops.isEmpty()) return false
        if (isSameFocusRequest(shops, currentLocation, focusRequestKey)) return true

        moveToFocusTarget(naverMap, shops, currentLocation)
        return true
    }

    fun focusCurrentLocation(
        naverMap: NaverMap,
        location: Location,
    ) {
        if (hasFocusedCurrentLocation) return
        hasFocusedCurrentLocation = true
        naverMap.moveCamera(
            CameraUpdate.scrollTo(
                LatLng(location.lat, location.lng),
            ),
        )
    }

    fun focusPlace(
        naverMap: NaverMap,
        location: Location,
        requestKey: Long,
    ) {
        if (requestKey == 0L || requestKey == lastPlaceFocusKey) return
        lastPlaceFocusKey = requestKey
        naverMap.locationTrackingMode = LocationTrackingMode.None
        naverMap.moveCamera(
            CameraUpdate.scrollAndZoomTo(
                LatLng(location.lat, location.lng),
                MapInteractionConfig.PLACE_SEARCH_ZOOM_LEVEL.toDouble(),
            ),
        )
    }

    private fun isSameFocusRequest(
        shops: RamenShops,
        currentLocation: Location?,
        focusRequestKey: Long,
    ): Boolean {
        val key = buildFocusKey(shops, currentLocation, focusRequestKey)
        if (key == lastFocusKey) return true
        lastFocusKey = key
        return false
    }

    private fun buildFocusKey(
        shops: RamenShops,
        currentLocation: Location?,
        focusRequestKey: Long,
    ): String =
        shops.values.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" } +
            ":${currentLocation != null}:$focusRequestKey"

    private fun moveToFocusTarget(
        naverMap: NaverMap,
        shops: RamenShops,
        currentLocation: Location?,
    ) {
        naverMap.locationTrackingMode = LocationTrackingMode.None
        when {
            shops.size == 1 -> moveToShop(naverMap, shops.values.first())
            currentLocation != null -> moveToNearestShop(naverMap, shops, currentLocation)
            else -> moveToShopsBounds(naverMap, shops)
        }
    }

    private fun moveToNearestShop(
        naverMap: NaverMap,
        shops: RamenShops,
        currentLocation: Location,
    ) {
        val nearest = shops.nearestTo(currentLocation)
        if (nearest != null) {
            moveToShop(naverMap, nearest)
        } else {
            moveToShopsBounds(naverMap, shops)
        }
    }

    private fun moveToShop(
        naverMap: NaverMap,
        shop: RamenShop,
    ) {
        naverMap.moveCamera(
            CameraUpdate.scrollAndZoomTo(
                LatLng(shop.location.lat, shop.location.lng),
                MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toDouble(),
            ),
        )
    }

    private fun moveToShopsBounds(
        naverMap: NaverMap,
        shops: RamenShops,
    ) {
        val bounds =
            LatLngBounds
                .Builder()
                .apply {
                    shops.values.forEach { include(LatLng(it.location.lat, it.location.lng)) }
                }.build()
        naverMap.moveCamera(CameraUpdate.fitBounds(bounds, FOCUS_PADDING_PX))
    }

    private companion object {
        private const val FOCUS_PADDING_PX = 120
    }
}
