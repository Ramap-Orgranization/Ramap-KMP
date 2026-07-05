@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.peto.ramap.ui.map

import android.location.Location
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdate
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.nearestTo
import com.peto.ramap.domain.model.Location as DomainLocation

/**
 * 카카오 지도 카메라 이동을 담당한다.
 *
 * 선택된 매장이 하나일 때는 중심 이동을, 여러 매장을 포커스할 때는 모든 매장이 보이도록
 * 카메라 영역을 맞춘다.
 */
internal actual class KakaoCameraController actual constructor() {
    private var lastFocusKey = ""

    /**
     * Compose 재구성으로 같은 포커스 요청이 반복될 수 있으므로,
     * 매장 id와 좌표로 만든 안정적인 key를 저장해 불필요한 카메라 이동을 막는다.
     */
    fun focusRamenShops(
        kakaoMap: KakaoMap,
        shops: List<RamenShop>,
        currentLocation: Location? = null,
    ) {
        val focusKey = focusKey(shops, currentLocation != null)
        if (focusKey.isBlank()) return
        if (lastFocusKey == focusKey) return

        lastFocusKey = focusKey

        when (shops.size) {
            1 -> moveToShop(kakaoMap, shops.first())
            else -> {
                val nearestShop = shops.nearestTo(currentLocation?.toDomainLocation())

                if (nearestShop != null) {
                    moveToShop(kakaoMap, nearestShop)
                } else {
                    fitShops(kakaoMap, shops)
                }
            }
        }
    }

    fun moveToLocation(
        kakaoMap: KakaoMap,
        location: Location,
    ) {
        kakaoMap.moveCamera(
            CameraUpdateFactory.newCenterPosition(
                LatLng.from(
                    location.latitude,
                    location.longitude,
                ),
            ),
        )
    }

    fun moveToShop(
        kakaoMap: KakaoMap,
        shop: RamenShop,
    ) {
        val position: LatLng = shopLatLng(shop)
        val shouldZoomIn = kakaoMap.zoomLevel < MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL
        val cameraUpdate = buildCameraMovement(shouldZoomIn, position)
        kakaoMap.moveCamera(cameraUpdate)
    }

    private fun buildCameraMovement(
        shouldZoomIn: Boolean,
        position: LatLng,
    ): CameraUpdate {
        if (shouldZoomIn) {
            return CameraUpdateFactory.newCenterPosition(
                position,
                MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL,
            )
        }
        return CameraUpdateFactory.newCenterPosition(position)
    }

    private fun fitShops(
        kakaoMap: KakaoMap,
        shops: List<RamenShop>,
    ) {
        kakaoMap.moveCamera(
            CameraUpdateFactory.fitMapPoints(
                shops.map(::shopLatLng).toTypedArray(),
                FOCUS_SHOPS_PADDING_PX,
            ),
        )
    }

    private fun shopLatLng(shop: RamenShop): LatLng =
        LatLng.from(
            shop.location.lat,
            shop.location.lng,
        )

    private fun focusKey(
        shops: List<RamenShop>,
        focusNearestToCurrentLocation: Boolean,
    ): String {
        if (shops.isEmpty()) return ""

        return shops.joinToString(separator = "|") { shop ->
            "${shop.id}:${shop.location.lat}:${shop.location.lng}"
        } + ":$focusNearestToCurrentLocation"
    }

    private fun Location.toDomainLocation(): DomainLocation =
        DomainLocation(
            lat = latitude,
            lng = longitude,
        )

    private companion object {
        private const val FOCUS_SHOPS_PADDING_PX = 120
    }
}
