@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.peto.ramap.ui.map

import cocoapods.KakaoMapsSDK.AreaRect
import cocoapods.KakaoMapsSDK.CameraUpdate
import cocoapods.KakaoMapsSDK.KakaoMap
import cocoapods.KakaoMapsSDK.MapPoint
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.Location
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.nearestTo
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
internal actual class KakaoCameraController {
    private var lastFocusKey = EMPTY_FOCUS_KEY

    fun focusRamenShops(
        kakaoMap: KakaoMap,
        shops: List<RamenShop>,
        currentLocation: Location? = null,
        focusRequestKey: Long = 0,
    ) {
        val focusKey = shops.focusKey(currentLocation != null, focusRequestKey)
        if (focusKey.isBlank()) return
        if (lastFocusKey == focusKey) return

        lastFocusKey = focusKey

        when (shops.size) {
            1 -> moveToShop(kakaoMap, shops.first())
            else -> {
                val nearestShop = shops.nearestTo(currentLocation)

                if (nearestShop != null) {
                    moveToShop(kakaoMap, nearestShop)
                } else {
                    fitShops(kakaoMap, shops)
                }
            }
        }
    }

    fun moveToCoordinate(
        kakaoMap: KakaoMap,
        coordinate: IosMapCoordinate,
    ) {
        moveCamera(
            kakaoMap = kakaoMap,
            cameraUpdate =
                CameraUpdate.makeWithTarget(
                    target = coordinate.toMapPoint(),
                    mapView = kakaoMap,
                ),
        )
    }

    fun moveToLocation(
        kakaoMap: KakaoMap,
        location: Location,
    ) {
        moveToCoordinate(
            kakaoMap = kakaoMap,
            coordinate =
                IosMapCoordinate(
                    latitude = location.lat,
                    longitude = location.lng,
                ),
        )
    }

    private fun moveToShop(
        kakaoMap: KakaoMap,
        shop: RamenShop,
    ) {
        val target = shop.toMapPoint()
        moveCamera(
            kakaoMap = kakaoMap,
            cameraUpdate =
                CameraUpdate.makeWithTarget(
                    target = target,
                    zoomLevel = MapInteractionConfig.SELECTED_MARKER_ZOOM_LEVEL.toLong(),
                    mapView = kakaoMap,
                ),
        )
    }

    private fun fitShops(
        kakaoMap: KakaoMap,
        shops: List<RamenShop>,
    ) {
        moveCamera(
            kakaoMap = kakaoMap,
            cameraUpdate =
                CameraUpdate.makeWithArea(
                    area = AreaRect(points = shops.map { shop -> shop.toMapPoint() }),
                    levelLimit = -1,
                ),
        )
    }

    private fun moveCamera(
        kakaoMap: KakaoMap,
        cameraUpdate: CameraUpdate,
    ) {
        kakaoMap.moveCamera(
            cameraUpdate,
            callback = null,
        )
    }

    private fun RamenShop.toMapPoint(): MapPoint =
        MapPoint(
            longitude = location.lng,
            latitude = location.lat,
        )

    private fun IosMapCoordinate.toMapPoint(): MapPoint =
        MapPoint(
            longitude = longitude,
            latitude = latitude,
        )

    private fun List<RamenShop>.focusKey(
        focusNearestToCurrentLocation: Boolean,
        focusRequestKey: Long,
    ): String {
        if (isEmpty()) return EMPTY_FOCUS_KEY

        return joinToString(separator = "|") { shop ->
            "${shop.id}:${shop.location.lat}:${shop.location.lng}"
        } + ":$focusNearestToCurrentLocation:$focusRequestKey"
    }

    private companion object {
        private const val EMPTY_FOCUS_KEY = ""
    }
}
