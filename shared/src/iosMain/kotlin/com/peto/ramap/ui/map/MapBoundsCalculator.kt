@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.peto.ramap.ui.map

import cocoapods.KakaoMapsSDK.KakaoMap
import com.peto.ramap.domain.model.MapBounds
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake

@OptIn(ExperimentalForeignApi::class)
internal actual class MapBoundsCalculator {
    fun currentBounds(
        kakaoMap: KakaoMap,
        width: Double,
        height: Double,
    ): MapBounds? {
        if (width <= 0.0 || height <= 0.0) return null

        val points =
            listOf(
                kakaoMap.coordinateAt(CGPointMake(0.0, 0.0)),
                kakaoMap.coordinateAt(CGPointMake(width, 0.0)),
                kakaoMap.coordinateAt(CGPointMake(0.0, height)),
                kakaoMap.coordinateAt(CGPointMake(width, height)),
            )

        return MapBounds(
            minLat = points.minOf { it.latitude },
            maxLat = points.maxOf { it.latitude },
            minLng = points.minOf { it.longitude },
            maxLng = points.maxOf { it.longitude },
        )
    }

    private fun KakaoMap.coordinateAt(point: CValue<CGPoint>): IosMapCoordinate =
        getPosition(point)
            .wgsCoord
            .useContents {
                IosMapCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                )
            }
}
