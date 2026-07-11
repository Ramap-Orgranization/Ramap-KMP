package com.peto.ramap.ui.map

import com.naver.maps.geometry.LatLng
import com.naver.maps.geometry.LatLngBounds
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.NaverMap
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.nearestTo

internal class NaverCameraController {
    private var lastFocusKey = ""

    fun focusShops(
        naverMap: NaverMap,
        shops: List<RamenShop>,
        currentLocation: Location?,
        focusRequestKey: Long,
    ) {
        if (shops.isEmpty()) return
        if (isSameFocusRequest(shops, currentLocation, focusRequestKey)) return

        moveToFocusTarget(naverMap, shops, currentLocation)
    }

    private fun isSameFocusRequest(
        shops: List<RamenShop>,
        currentLocation: Location?,
        focusRequestKey: Long,
    ): Boolean {
        val key = buildFocusKey(shops, currentLocation, focusRequestKey)
        if (key == lastFocusKey) return true
        lastFocusKey = key
        return false
    }

    private fun buildFocusKey(
        shops: List<RamenShop>,
        currentLocation: Location?,
        focusRequestKey: Long,
    ): String =
        shops.joinToString("|") { "${it.id}:${it.location.lat}:${it.location.lng}" } +
            ":${currentLocation != null}:$focusRequestKey"

    private fun moveToFocusTarget(
        naverMap: NaverMap,
        shops: List<RamenShop>,
        currentLocation: Location?,
    ) {
        when {
            shops.size == 1 -> moveToShop(naverMap, shops.first())
            currentLocation != null -> moveToNearestShop(naverMap, shops, currentLocation)
            else -> moveToShopsBounds(naverMap, shops)
        }
    }

    private fun moveToNearestShop(
        naverMap: NaverMap,
        shops: List<RamenShop>,
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
        shops: List<RamenShop>,
    ) {
        val bounds =
            LatLngBounds
                .Builder()
                .apply {
                    shops.forEach { include(LatLng(it.location.lat, it.location.lng)) }
                }.build()
        naverMap.moveCamera(CameraUpdate.fitBounds(bounds, FOCUS_PADDING_PX))
    }

    private companion object {
        private const val FOCUS_PADDING_PX = 120
    }
}
