@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

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
import com.peto.ramap.core.config.MarkerConfig
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.domain.model.RamenShops
import com.peto.ramap.ui.extension.alphaComponent
import com.peto.ramap.ui.extension.blueComponent
import com.peto.ramap.ui.extension.greenComponent
import com.peto.ramap.ui.extension.redComponent
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGBlendMode
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.Foundation.NSBundle
import platform.UIKit.UIColor
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage

internal actual class KakaoMarkerRenderer {
    private val renderedShopIds = mutableSetOf<String>()
    private val shopsByPoiId = mutableMapOf<String, RamenShop>()

    fun render(
        kakaoMap: KakaoMap,
        shops: RamenShops,
        onShopClick: (RamenShop) -> Unit,
    ) {
        val layer = prepareMarkerLayer(kakaoMap) ?: return
        val shopList = shops.values.toList()

        removeStaleMarkers(
            layer = layer,
            currentShopIds = shops.keys,
        )
        removeChangedMarkers(
            layer = layer,
            shops = shopList,
        )
        renderNewMarkers(
            layer = layer,
            shops = shopList,
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
    ) {
        shopsByPoiId[poiId]?.let(onShopClick)
    }

    fun selectNearestShopAt(
        kakaoMap: KakaoMap,
        point: CValue<CGPoint>,
        onShopClick: (RamenShop) -> Unit,
    ) {
        val tappedCoordinate = kakaoMap.coordinateAt(point)
        val shop = findNearestShop(tappedCoordinate) ?: return

        onShopClick(shop)
    }

    fun clear() {
        renderedShopIds.clear()
        shopsByPoiId.clear()
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
        currentShopIds: Set<String>,
    ) {
        val staleShopIds = renderedShopIds - currentShopIds
        if (staleShopIds.isEmpty()) return

        val stalePoiIds = staleShopIds.map { shopId -> shopId.toMarkerPoiId() }
        layer.removePoisWithPoiIDs(stalePoiIds, callback = null)
        stalePoiIds.forEach(shopsByPoiId::remove)
        renderedShopIds.removeAll(staleShopIds)
    }

    private fun removeChangedMarkers(
        layer: LabelLayer,
        shops: List<RamenShop>,
    ) {
        val changedShopIds =
            shops
                .filter { shop ->
                    val renderedShop = shopsByPoiId[shop.id.toMarkerPoiId()]
                    renderedShop != null && renderedShop != shop
                }.map { shop -> shop.id }

        if (changedShopIds.isEmpty()) return

        val changedPoiIds = changedShopIds.map { shopId -> shopId.toMarkerPoiId() }
        layer.removePoisWithPoiIDs(changedPoiIds, callback = null)
        changedPoiIds.forEach(shopsByPoiId::remove)
        renderedShopIds.removeAll(changedShopIds)
    }

    private fun renderNewMarkers(
        layer: LabelLayer,
        shops: List<RamenShop>,
    ) {
        val newShops = shops.filter { shop -> shop.id !in renderedShopIds }
        if (newShops.isEmpty()) return

        newShops.forEach { shop -> addMarkerPoi(layer, shop) }
    }

    private fun addMarkerPoi(
        layer: LabelLayer,
        shop: RamenShop,
    ) {
        val poiId = shop.id.toMarkerPoiId()
        val poi =
            layer.addPoiWithOption(
                option = createMarkerPoiOptions(shop, poiId),
                at = shop.toMapPoint(),
                callback = null,
            )

        poi?.show()
        poi?.clickable = true

        if (poi == null) return

        shopsByPoiId[poiId] = shop
        renderedShopIds += shop.id
    }

    private fun createMarkerPoiOptions(
        shop: RamenShop,
        poiId: String,
    ): PoiOptions =
        PoiOptions(
            styleID = if (shop.isVisible) MarkerConfig.Single.STYLE_ID else MarkerConfig.Single.HIDDEN_STYLE_ID,
            poiID = poiId,
        ).apply {
            clickable = true
            addText(
                PoiText(
                    text = shop.name,
                    styleIndex = 0u,
                ),
            )
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
        imageNamed(MY_LOCATION_IMAGE_NAME)?.let { image ->
            PoiIconStyle(
                image,
                CGPointMake(0.5, 0.5),
                poiTransition(),
                true,
                true,
                null,
            )
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

    private fun findNearestShop(tappedCoordinate: IosMapCoordinate): RamenShop? =
        shopsByPoiId
            .values
            .map { shop ->
                shop to
                    tappedCoordinate.distanceTo(
                        shop.location.lat,
                        shop.location.lng,
                    )
            }.minByOrNull { (_, distance) -> distance }
            ?.takeIf { (_, distance) -> distance <= MARKER_TAP_RADIUS_METERS }
            ?.first

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

    private fun String.toMarkerPoiId(): String = "ramen-shop-$this"

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
        private const val MARKER_IMAGE_NAME = "marker_ramen"
        private const val MARKER_LAYER_ID = "ramen-shop-marker-layer"
        private const val MY_LOCATION_LAYER_ID = "my-location-marker-layer"
        private const val MARKER_LAYER_Z_ORDER = 10L
        private const val MY_LOCATION_LAYER_Z_ORDER = 20L
        private const val MY_LOCATION_IMAGE_NAME = "my_location_marker"
        private const val MY_LOCATION_STYLE_ID = "my-location-marker-style"
        private const val MY_LOCATION_POI_ID = "my-location-marker"
        private const val MARKER_TAP_RADIUS_METERS = 80.0
        private const val VISIBLE_MARKER_ALPHA = 1.0
    }
}
