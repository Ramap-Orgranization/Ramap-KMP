@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.map.marker

import cocoapods.KakaoMapsSDK.LabelManager
import cocoapods.KakaoMapsSDK.PerLevelPoiStyle
import cocoapods.KakaoMapsSDK.PoiIconStyle
import cocoapods.KakaoMapsSDK.PoiOptions
import cocoapods.KakaoMapsSDK.PoiStyle
import cocoapods.KakaoMapsSDK.PoiText
import cocoapods.KakaoMapsSDK.PoiTextLineStyle
import cocoapods.KakaoMapsSDK.PoiTextStyle
import cocoapods.KakaoMapsSDK.PoiTransition
import cocoapods.KakaoMapsSDK.TextStyle
import cocoapods.KakaoMapsSDK.TransitionTypeNone
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.core.config.MarkerClusterConfig
import com.peto.ramap.core.config.MarkerConfig
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.ui.extension.alphaComponent
import com.peto.ramap.ui.extension.blueComponent
import com.peto.ramap.ui.extension.greenComponent
import com.peto.ramap.ui.extension.redComponent
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGBlendMode
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSBundle
import platform.UIKit.NSTextAlignmentCenter
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UILabel

/**
 * 마커를 iOS KakaoMap SDK poi option과 style로 변환한다.
 */
internal class IosMarkerOptionFactory(
    private val labelManager: LabelManager,
    private val clusterMarkerImages: MutableMap<String, UIImage>,
) {
    fun poiOptions(
        entry: MarkerRenderEntry,
        poiId: String,
    ): PoiOptions? {
        val styleId = markerStyleId(entry.marker) ?: return null
        return PoiOptions(
            styleID = styleId,
            poiID = poiId,
        ).apply {
            clickable = true
            if (entry.marker is Marker.SingleMarker) {
                addText(
                    PoiText(
                        text = entry.marker.shop.name,
                        styleIndex = 0u,
                    ),
                )
            }
        }
    }

    fun poiId(markerKey: String): String = "ramen-${markerKey.hashCode().toUInt().toString(radix = 16)}"

    private fun markerStyleId(marker: Marker): String? =
        when (marker) {
            is Marker.SingleMarker -> {
                if (!ensureMarkerStyles()) {
                    null
                } else if (marker.shop.isVisible) {
                    MarkerConfig.Single.STYLE_ID
                } else {
                    MarkerConfig.Single.HIDDEN_STYLE_ID
                }
            }
            is Marker.ClusterMaker -> ensureClusterMarkerStyle(marker.count)
        }

    private fun ensureMarkerStyles(): Boolean =
        addMarkerStyle(
            styleId = MarkerConfig.Single.STYLE_ID,
            alpha = VISIBLE_MARKER_ALPHA,
        ) &&
            addMarkerStyle(
                styleId = MarkerConfig.Single.HIDDEN_STYLE_ID,
                alpha = MapInteractionConfig.HIDDEN_SHOP_ALPHA.toDouble(),
            )

    private fun ensureClusterMarkerStyle(count: Int): String? {
        val text = MarkerClusterConfig.countText(count)
        val styleId = "${MarkerConfig.Cluster.STYLE_ID}-$text"
        val markerIconStyle = createClusterMarkerIconStyle(text) ?: return null
        val poiStyle =
            PoiStyle(
                styleId,
                listOf(
                    PerLevelPoiStyle(
                        markerIconStyle,
                        0.0f,
                        0,
                    ),
                ),
            )
        labelManager.addPoiStyle(poiStyle)
        return styleId
    }

    private fun addMarkerStyle(
        styleId: String,
        alpha: Double,
    ): Boolean {
        val markerIconStyle = createMarkerIconStyle(alpha) ?: return false
        val poiStyle =
            PoiStyle(
                styleId,
                listOf(
                    PerLevelPoiStyle(
                        markerIconStyle,
                        createMarkerTextStyle(alpha),
                        0.0f,
                        0,
                    ),
                ),
            )

        labelManager.addPoiStyle(poiStyle)
        return true
    }

    private fun createClusterMarkerIconStyle(text: String): PoiIconStyle? {
        val image = clusterMarkerImage(text) ?: return null

        return PoiIconStyle(
            image,
            CGPointMake(0.5, 0.5),
            poiTransition(),
            true,
            true,
            null,
        )
    }

    private fun clusterMarkerImage(text: String): UIImage? =
        clusterMarkerImages.getOrPut(text) {
            val baseImage = imageNamed(CLUSTER_MARKER_IMAGE_NAME) ?: return null
            UIGraphicsBeginImageContextWithOptions(
                CGSizeMake(CLUSTER_MARKER_SIZE, CLUSTER_MARKER_SIZE),
                false,
                0.0,
            )
            baseImage.drawInRect(CGRectMake(0.0, 0.0, CLUSTER_MARKER_SIZE, CLUSTER_MARKER_SIZE))
            UILabel(frame = CGRectZero.readValue())
                .apply {
                    setFrame(
                        CGRectMake(
                            0.0,
                            0.0,
                            CLUSTER_MARKER_CONTENT_SIZE,
                            CLUSTER_MARKER_CONTENT_SIZE,
                        ),
                    )
                    this.text = text
                    textAlignment = NSTextAlignmentCenter
                    textColor = UIColor.whiteColor
                    font = UIFont.boldSystemFontOfSize(CLUSTER_MARKER_TEXT_SIZE)
                    backgroundColor = UIColor.clearColor
                }.drawTextInRect(
                    CGRectMake(
                        0.0,
                        0.0,
                        CLUSTER_MARKER_CONTENT_SIZE,
                        CLUSTER_MARKER_CONTENT_SIZE,
                    ),
                )

            val image = UIGraphicsGetImageFromCurrentImageContext()
            UIGraphicsEndImageContext()
            image ?: baseImage
        }

    private fun createMarkerIconStyle(alpha: Double): PoiIconStyle? {
        val image = imageNamed(MARKER_IMAGE_NAME) ?: return null

        return PoiIconStyle(
            image.withAlpha(alpha),
            CGPointMake(0.5, 1.0),
            poiTransition(),
            true,
            true,
            null,
        )
    }

    private fun createMarkerTextStyle(alpha: Double): PoiTextStyle =
        PoiTextStyle(
            poiTransition(),
            true,
            true,
            listOf(createMarkerTextLineStyle(alpha)),
        )

    private fun createMarkerTextLineStyle(alpha: Double): PoiTextLineStyle =
        PoiTextLineStyle()
            .apply {
                textStyle =
                    TextStyle(
                        MarkerConfig.Single.LABEL_TEXT_SIZE.toULong(),
                        markerTextColor(alpha),
                        MarkerConfig.Single.LABEL_STROKE_WIDTH.toULong(),
                        UIColor.whiteColor,
                        "",
                        0,
                        1.0f,
                        1.0f,
                    )
            }

    private fun markerTextColor(alpha: Double): UIColor =
        UIColor.colorWithRed(
            red = MarkerConfig.Single.LABEL_TEXT_COLOR.redComponent(),
            green = MarkerConfig.Single.LABEL_TEXT_COLOR.greenComponent(),
            blue = MarkerConfig.Single.LABEL_TEXT_COLOR.blueComponent(),
            alpha = MarkerConfig.Single.LABEL_TEXT_COLOR.alphaComponent() * alpha,
        )

    private fun imageNamed(name: String): UIImage? =
        UIImage.imageNamed(name)
            ?: NSBundle.mainBundle
                .pathForResource(
                    name = name,
                    ofType = "png",
                )?.let(UIImage.Companion::imageWithContentsOfFile)

    private fun poiTransition(): CValue<PoiTransition> =
        cValue {
            entrance = TransitionTypeNone
            exit = TransitionTypeNone
        }

    private fun UIImage.withAlpha(alpha: Double): UIImage {
        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
        drawAtPoint(
            point = CGPointMake(0.0, 0.0),
            blendMode = CGBlendMode.kCGBlendModeNormal,
            alpha = alpha,
        )
        val image = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return image ?: this
    }

    private companion object {
        private const val CLUSTER_MARKER_SIZE = 36.0
        private const val CLUSTER_MARKER_CONTENT_SIZE = CLUSTER_MARKER_SIZE * 70.0 / 76.0
        private const val CLUSTER_MARKER_TEXT_SIZE = 14.0
        private const val MARKER_IMAGE_NAME = "marker_ramen"
        private const val CLUSTER_MARKER_IMAGE_NAME = "cluster_marker"
        private const val VISIBLE_MARKER_ALPHA = 1.0
    }
}
