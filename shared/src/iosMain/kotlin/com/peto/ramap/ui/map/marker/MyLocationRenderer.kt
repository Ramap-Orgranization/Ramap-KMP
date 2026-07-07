@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.map.marker

import cocoapods.KakaoMapsSDK.CompetitionTypeNone
import cocoapods.KakaoMapsSDK.CompetitionUnitPoi
import cocoapods.KakaoMapsSDK.LabelLayer
import cocoapods.KakaoMapsSDK.LabelLayerOptions
import cocoapods.KakaoMapsSDK.LabelManager
import cocoapods.KakaoMapsSDK.MapPoint
import cocoapods.KakaoMapsSDK.OrderingTypeRank
import cocoapods.KakaoMapsSDK.PerLevelPoiStyle
import cocoapods.KakaoMapsSDK.PoiIconStyle
import cocoapods.KakaoMapsSDK.PoiOptions
import cocoapods.KakaoMapsSDK.PoiStyle
import cocoapods.KakaoMapsSDK.PoiTransition
import cocoapods.KakaoMapsSDK.TransitionTypeNone
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage

/**
 * iOS KakaoMap 현재 위치 마커를 렌더링한다.
 */
internal class MyLocationRenderer {
    private var myLocationMarkerImage: UIImage? = null

    fun render(
        labelManager: LabelManager,
        coordinate: IosMapCoordinate,
    ) {
        val layer = prepareMyLocationLayer(labelManager) ?: return

        layer.removePoisWithPoiIDs(listOf(MY_LOCATION_POI_ID), callback = null)
        layer
            .addPoiWithOption(
                option =
                    PoiOptions(
                        styleID = MY_LOCATION_STYLE_ID,
                        poiID = MY_LOCATION_POI_ID,
                    ).apply {
                        clickable = false
                    },
                at = coordinate.toMapPoint(),
                callback = null,
            )?.show()
    }

    private fun prepareMyLocationLayer(labelManager: LabelManager): LabelLayer? {
        if (!ensureMyLocationStyle(labelManager)) return null

        return (
            labelManager.getLabelLayerWithLayerID(MY_LOCATION_LAYER_ID)
                ?: labelManager.addLabelLayerWithOption(createMyLocationLayerOptions())
        )?.apply {
            visible = true
            setClickable(false)
        }
    }

    private fun ensureMyLocationStyle(labelManager: LabelManager): Boolean {
        val markerIconStyle = createMyLocationIconStyle() ?: return false
        val poiStyle =
            PoiStyle(
                MY_LOCATION_STYLE_ID,
                listOf(
                    PerLevelPoiStyle(
                        markerIconStyle,
                        0.0f,
                        0,
                    ),
                ),
            )

        labelManager.addPoiStyle(poiStyle)
        return true
    }

    private fun createMyLocationIconStyle(): PoiIconStyle? =
        scaledMyLocationMarkerImage()?.let { image ->
            PoiIconStyle(
                image,
                CGPointMake(0.5, 0.5),
                poiTransition(),
                true,
                true,
                null,
            )
        }

    private fun scaledMyLocationMarkerImage(): UIImage? {
        myLocationMarkerImage?.let { return it }

        val baseImage = imageNamed(MY_LOCATION_IMAGE_NAME) ?: return null
        UIGraphicsBeginImageContextWithOptions(
            CGSizeMake(MY_LOCATION_MARKER_SIZE, MY_LOCATION_MARKER_SIZE),
            false,
            0.0,
        )
        baseImage.drawInRect(CGRectMake(0.0, 0.0, MY_LOCATION_MARKER_SIZE, MY_LOCATION_MARKER_SIZE))
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return (image ?: baseImage).also { myLocationMarkerImage = it }
    }

    private fun imageNamed(name: String): UIImage? =
        UIImage.imageNamed(name)
            ?: NSBundle.mainBundle
                .pathForResource(
                    name = name,
                    ofType = "png",
                )?.let(UIImage.Companion::imageWithContentsOfFile)

    private fun createMyLocationLayerOptions(): LabelLayerOptions =
        LabelLayerOptions(
            MY_LOCATION_LAYER_ID,
            CompetitionTypeNone,
            CompetitionUnitPoi,
            OrderingTypeRank,
            MY_LOCATION_LAYER_Z_ORDER,
        )

    private fun IosMapCoordinate.toMapPoint(): MapPoint =
        MapPoint(
            longitude = longitude,
            latitude = latitude,
        )

    private fun poiTransition(): CValue<PoiTransition> =
        cValue {
            entrance = TransitionTypeNone
            exit = TransitionTypeNone
        }

    private companion object {
        private const val MY_LOCATION_LAYER_ID = "my-location-marker-layer"
        private const val MY_LOCATION_LAYER_Z_ORDER = 20L
        private const val MY_LOCATION_MARKER_SIZE = 16.0
        private const val MY_LOCATION_IMAGE_NAME = "my_location_marker"
        private const val MY_LOCATION_STYLE_ID = "my-location-marker-style-v2"
        private const val MY_LOCATION_POI_ID = "my-location-marker"
    }
}
