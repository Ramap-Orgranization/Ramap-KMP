@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.peto.ramap.ui.map.marker

import cocoapods.KakaoMapsSDK.CompetitionTypeNone
import cocoapods.KakaoMapsSDK.CompetitionUnitPoi
import cocoapods.KakaoMapsSDK.KakaoMap
import cocoapods.KakaoMapsSDK.LabelLayer
import cocoapods.KakaoMapsSDK.LabelLayerOptions
import cocoapods.KakaoMapsSDK.MapPoint
import cocoapods.KakaoMapsSDK.OrderingTypeRank
import com.peto.ramap.domain.model.Marker
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.ui.model.IosMapCoordinate
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.UIKit.UIImage

/**
 * iOS KakaoMap 매장/클러스터 마커 렌더링 플로우를 조정한다.
 */
internal class ShopMarkerRenderer {
    private val renderCoordinator = MarkerRenderCoordinator(MarkerRenderKeyPolicy())
    private val markersByPoiId = mutableMapOf<String, Marker>()
    private val clusterMarkerImages = mutableMapOf<String, UIImage>()

    fun render(
        kakaoMap: KakaoMap,
        markers: List<Marker>,
        onShopClick: (RamenShop) -> Unit,
        onClusterClick: (Marker.ClusterMaker) -> Unit,
    ) {
        val layer = prepareMarkerLayer(kakaoMap) ?: return
        val optionFactory =
            IosMarkerOptionFactory(
                labelManager = kakaoMap.getLabelManager(),
                clusterMarkerImages = clusterMarkerImages,
            )

        renderCoordinator.render(
            markers = markers,
            action =
                IosMarkerRenderAction(
                    layer = layer,
                    optionFactory = optionFactory,
                    markersByPoiId = markersByPoiId,
                ),
        )

        layer.setClickable(true)
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
        renderCoordinator.clear()
        markersByPoiId.clear()
    }

    private fun prepareMarkerLayer(kakaoMap: KakaoMap): LabelLayer? =
        (
            kakaoMap
                .getLabelManager()
                .getLabelLayerWithLayerID(MARKER_LAYER_ID)
                ?: kakaoMap
                    .getLabelManager()
                    .addLabelLayerWithOption(createMarkerLayerOptions())
        )?.apply {
            visible = true
            setClickable(true)
        }

    private fun createMarkerLayerOptions(): LabelLayerOptions =
        LabelLayerOptions(
            MARKER_LAYER_ID,
            CompetitionTypeNone,
            CompetitionUnitPoi,
            OrderingTypeRank,
            MARKER_LAYER_Z_ORDER,
        )

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

    private fun KakaoMap.coordinateAt(point: CValue<CGPoint>): IosMapCoordinate =
        getPosition(point)
            .wgsCoord
            .useContents {
                IosMapCoordinate(
                    latitude = latitude,
                    longitude = longitude,
                )
            }

    private companion object {
        private const val MARKER_LAYER_ID = "ramen-shop-marker-layer"
        private const val MARKER_LAYER_Z_ORDER = 10L
        private const val MARKER_TAP_RADIUS_METERS = 80.0
    }
}

internal fun Marker.toMapPoint(): MapPoint =
    MapPoint(
        longitude = location.lng,
        latitude = location.lat,
    )
