@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.peto.ramap.ui.map

import cocoapods.KakaoMapsSDK.CompetitionTypeNone
import cocoapods.KakaoMapsSDK.CompetitionUnitPoi
import cocoapods.KakaoMapsSDK.KakaoMap
import cocoapods.KakaoMapsSDK.LabelLayer
import cocoapods.KakaoMapsSDK.LabelLayerOptions
import cocoapods.KakaoMapsSDK.LabelManager
import cocoapods.KakaoMapsSDK.MapPoint
import cocoapods.KakaoMapsSDK.OrderingTypeRank
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
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.ui.extension.alphaComponent
import com.peto.ramap.ui.extension.blueComponent
import com.peto.ramap.ui.extension.greenComponent
import com.peto.ramap.ui.extension.redComponent
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGBlendMode
import platform.CoreGraphics.CGPoint
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

internal actual class KakaoMarkerRenderer actual constructor() {
    private val renderedMarkerKeys = mutableSetOf<String>()
    private val markersByPoiId = mutableMapOf<String, Marker>()
    private val clusterMarkerImages = mutableMapOf<String, UIImage>()
    private var myLocationMarkerImage: UIImage? = null

    fun render(
        kakaoMap: KakaoMap,
        markers: List<Marker>,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        val layer = prepareMarkerLayer(kakaoMap) ?: return
        val markersByKey = markers.associateBy(::markerKey)

        removeStaleMarkers(
            layer = layer,
            currentMarkerKeys = markersByKey.keys,
        )
        removeChangedMarkers(
            layer = layer,
            markersByKey = markersByKey,
        )
        renderNewMarkers(
            labelManager = kakaoMap.getLabelManager(),
            layer = layer,
            markers = markers,
        )
        layer.setClickable(true)
    }

    fun renderMyLocation(
        kakaoMap: KakaoMap,
        coordinate: IosMapCoordinate,
    ) {
        val labelManager = kakaoMap.getLabelManager()
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

    fun handlePoiTap(
        poiId: String,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        handleMarkerTap(
            marker = markersByPoiId[poiId],
            onShopClick = onShopClick,
            onClusterClick = onClusterClick,
        )
    }

    fun selectNearestShopAt(
        kakaoMap: KakaoMap,
        point: CValue<CGPoint>,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        val tappedCoordinate = kakaoMap.coordinateAt(point)
        val marker = findNearestMarker(tappedCoordinate) ?: return

        handleMarkerTap(
            marker = marker,
            onShopClick = onShopClick,
            onClusterClick = onClusterClick,
        )
    }

    fun clear() {
        renderedMarkerKeys.clear()
        markersByPoiId.clear()
    }

    private fun prepareMarkerLayer(kakaoMap: KakaoMap): LabelLayer? {
        val labelManager = kakaoMap.getLabelManager()
        if (!ensureMarkerStyles(labelManager)) return null

        return (
            labelManager.getLabelLayerWithLayerID(MARKER_LAYER_ID)
                ?: labelManager.addLabelLayerWithOption(createMarkerLayerOptions())
        )?.apply {
            visible = true
            setClickable(true)
        }
    }

    private fun ensureMarkerStyles(labelManager: LabelManager): Boolean =
        addMarkerStyle(
            labelManager = labelManager,
            styleId = MarkerConfig.Single.STYLE_ID,
            alpha = VISIBLE_MARKER_ALPHA,
        ) &&
            addMarkerStyle(
                labelManager = labelManager,
                styleId = MarkerConfig.Single.HIDDEN_STYLE_ID,
                alpha = MapInteractionConfig.HIDDEN_SHOP_ALPHA.toDouble(),
            )

    private fun ensureClusterMarkerStyle(
        labelManager: LabelManager,
        count: Int,
    ): String? {
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
        labelManager: LabelManager,
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

    private fun createMarkerLayerOptions(): LabelLayerOptions =
        LabelLayerOptions(
            MARKER_LAYER_ID,
            CompetitionTypeNone,
            CompetitionUnitPoi,
            OrderingTypeRank,
            MARKER_LAYER_Z_ORDER,
        )

    private fun removeStaleMarkers(
        layer: LabelLayer,
        currentMarkerKeys: Set<String>,
    ) {
        val staleMarkerKeys = renderedMarkerKeys - currentMarkerKeys
        if (staleMarkerKeys.isEmpty()) return

        val stalePoiIds = staleMarkerKeys.map(::markerPoiId)
        layer.removePoisWithPoiIDs(stalePoiIds, callback = null)
        stalePoiIds.forEach(markersByPoiId::remove)
        renderedMarkerKeys.removeAll(staleMarkerKeys)
    }

    private fun removeChangedMarkers(
        layer: LabelLayer,
        markersByKey: Map<String, Marker>,
    ) {
        val changedMarkerKeys =
            markersByKey
                .filter { (markerKey, marker) ->
                    val renderedMarker = markersByPoiId[markerPoiId(markerKey)]
                    renderedMarker != null && renderedMarker != marker
                }.keys

        if (changedMarkerKeys.isEmpty()) return

        val changedPoiIds = changedMarkerKeys.map(::markerPoiId)
        layer.removePoisWithPoiIDs(changedPoiIds, callback = null)
        changedPoiIds.forEach(markersByPoiId::remove)
        renderedMarkerKeys.removeAll(changedMarkerKeys)
    }

    private fun renderNewMarkers(
        labelManager: LabelManager,
        layer: LabelLayer,
        markers: List<Marker>,
    ) {
        val newMarkers = markers.filter { marker -> markerKey(marker) !in renderedMarkerKeys }
        if (newMarkers.isEmpty()) return

        newMarkers.forEach { marker -> addMarkerPoi(labelManager, layer, marker) }
    }

    private fun addMarkerPoi(
        labelManager: LabelManager,
        layer: LabelLayer,
        marker: Marker,
    ) {
        val markerKey = markerKey(marker)
        val poiId = markerPoiId(markerKey)
        val poi =
            layer.addPoiWithOption(
                option = createMarkerPoiOptions(labelManager, marker, poiId) ?: return,
                at = marker.toMapPoint(),
                callback = null,
            )

        poi?.show()
        poi?.clickable = true

        if (poi == null) return

        markersByPoiId[poiId] = marker
        renderedMarkerKeys += markerKey
    }

    private fun createMarkerPoiOptions(
        labelManager: LabelManager,
        marker: Marker,
        poiId: String,
    ): PoiOptions? {
        val styleId = markerStyleId(labelManager, marker) ?: return null
        return PoiOptions(
            styleID = styleId,
            poiID = poiId,
        ).apply {
            clickable = true
            if (marker is Marker.SingleMarker) {
                addText(
                    PoiText(
                        text = marker.shop.name,
                        styleIndex = 0u,
                    ),
                )
            }
        }
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
                )?.let(UIImage::imageWithContentsOfFile)

    private fun createMyLocationLayerOptions(): LabelLayerOptions =
        LabelLayerOptions(
            MY_LOCATION_LAYER_ID,
            CompetitionTypeNone,
            CompetitionUnitPoi,
            OrderingTypeRank,
            MY_LOCATION_LAYER_Z_ORDER,
        )

    private fun KakaoMap.coordinateAt(point: CValue<CGPoint>): IosMapCoordinate =
        getPosition(point)
            .wgsCoord
            .useContents {
                IosMapCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                )
            }

    private fun findNearestMarker(tappedCoordinate: IosMapCoordinate): Marker? =
        markersByPoiId
            .values
            .map { marker ->
                marker to
                    tappedCoordinate.distanceTo(
                        marker.location.lat,
                        marker.location.lng,
                    )
            }.minByOrNull { (_, distance) -> distance }
            ?.takeIf { (_, distance) -> distance <= MARKER_TAP_RADIUS_METERS }
            ?.first

    private fun handleMarkerTap(
        marker: Marker?,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        when (marker) {
            is Marker.SingleMarker -> onShopClick(marker.shop)
            is Marker.ClusterMaker -> onClusterClick(marker)
            null -> Unit
        }
    }

    private fun markerStyleId(
        labelManager: LabelManager,
        marker: Marker,
    ): String? =
        when (marker) {
            is Marker.SingleMarker ->
                if (marker.shop.isVisible) {
                    MarkerConfig.Single.STYLE_ID
                } else {
                    MarkerConfig.Single.HIDDEN_STYLE_ID
                }
            is Marker.ClusterMaker -> ensureClusterMarkerStyle(labelManager, marker.count)
        }

    private fun markerKey(marker: Marker): String =
        when (marker) {
            is Marker.SingleMarker -> "shop:${marker.id}:${marker.shop.isVisible}"
            is Marker.ClusterMaker -> "cluster:${marker.id}"
        }

    private fun markerPoiId(markerKey: String): String = "ramen-${markerKey.hashCode().toUInt().toString(radix = 16)}"

    private fun Marker.toMapPoint(): MapPoint =
        MapPoint(
            longitude = location.lng,
            latitude = location.lat,
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
        private const val MARKER_LAYER_ID = "ramen-shop-marker-layer"
        private const val MY_LOCATION_LAYER_ID = "my-location-marker-layer"
        private const val MARKER_LAYER_Z_ORDER = 10L
        private const val MY_LOCATION_LAYER_Z_ORDER = 20L
        private const val MY_LOCATION_MARKER_SIZE = 16.0
        private const val MY_LOCATION_IMAGE_NAME = "my_location_marker"
        private const val MY_LOCATION_STYLE_ID = "my-location-marker-style-v2"
        private const val MY_LOCATION_POI_ID = "my-location-marker"
        private const val MARKER_TAP_RADIUS_METERS = 80.0
        private const val VISIBLE_MARKER_ALPHA = 1.0
    }
}
