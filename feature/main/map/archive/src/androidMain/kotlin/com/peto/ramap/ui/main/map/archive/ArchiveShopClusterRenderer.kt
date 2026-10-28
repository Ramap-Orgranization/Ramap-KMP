package com.peto.ramap.ui.main.map.archive

import android.graphics.PointF
import androidx.compose.ui.graphics.asAndroidBitmap
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.NaverMap
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.DefaultThresholdStrategy
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.archive.model.ArchiveMapInteractionConfig
import com.peto.ramap.ui.main.map.archive.model.MapClusteringOptions
import com.peto.ramap.ui.main.map.archive.model.MapMarkerImage
import com.peto.ramap.ui.main.map.archive.model.hasOnlyOverlappingMarkers
import com.peto.ramap.ui.main.map.archive.model.overlappingMarkerShops

internal class ArchiveShopClusterRenderer(
    clusteringOptions: MapClusteringOptions,
    private val onShopClick: (RamenShop) -> Unit,
    private val onClusterClick: (List<RamenShop>, Int, Int) -> Unit,
) {
    private var naverMap: NaverMap? = null
    private var markerImages: Map<String, MapMarkerImage> = emptyMap()
    private var renderedShops: Map<String, ArchiveShopKey> = emptyMap()
    private val clusterer =
        Clusterer
            .ComplexBuilder<ArchiveShopKey>()
            .maxClusteringZoom(clusteringOptions.maxClusteringZoomLevel)
            .maxIndexingZoom(clusteringOptions.maxClusteringZoomLevel)
            .thresholdStrategy(DefaultThresholdStrategy(clusteringOptions.screenDistanceDp))
            .maxScreenDistance(clusteringOptions.screenDistanceDp)
            .animationDuration(0)
            .tagMergeStrategy(ArchiveShopTagMergeStrategy())
            .clusterMarkerUpdater(
                object : DefaultClusterMarkerUpdater() {
                    override fun updateClusterMarker(
                        info: ClusterMarkerInfo,
                        marker: Marker,
                    ) {
                        super.updateClusterMarker(info, marker)
                        val shops = info.tag as? List<*> ?: return
                        val clusterShops = shops.filterIsInstance<RamenShop>()
                        if (clusterShops.size != info.size ||
                            !hasOnlyOverlappingMarkers(
                                clusterShops,
                            )
                        ) {
                            return
                        }
                        marker.onClickListener =
                            Overlay.OnClickListener {
                                val point = naverMap?.projection?.toScreenLocation(info.position)
                                onClusterClick(clusterShops, point?.x?.toInt() ?: 0, point?.y?.toInt() ?: 0)
                                true
                            }
                    }
                },
            ).leafMarkerUpdater(
                object : DefaultLeafMarkerUpdater() {
                    override fun updateLeafMarker(
                        info: LeafMarkerInfo,
                        marker: Marker,
                    ) {
                        super.updateLeafMarker(info, marker)
                        val shop = (info.key as ArchiveShopKey).shop
                        val custom = markerImages[shop.id] ?: return
                        marker.icon = OverlayImage.fromBitmap(custom.bitmap.asAndroidBitmap())
                        marker.anchor = PointF(0.5f, custom.anchorY)
                        marker.captionText = shop.name
                        marker.alpha = if (shop.isVisible) 1.0f else ArchiveMapInteractionConfig.HIDDEN_SHOP_ALPHA
                        marker.onClickListener =
                            Overlay.OnClickListener {
                                onLeafMarkerClick(shop)
                                true
                            }
                    }
                },
            ).build()

    private fun onLeafMarkerClick(shop: RamenShop) {
        val shops = overlappingMarkerShops(shop, renderedShops.values.map { it.shop })
        if (shops.size < 2) {
            onShopClick(shop)
            return
        }
        val point = naverMap?.projection?.toScreenLocation(LatLng(shop.location.lat, shop.location.lng))
        onClusterClick(shops, point?.x?.toInt() ?: 0, point?.y?.toInt() ?: 0)
    }

    fun render(
        naverMap: NaverMap,
        shops: RamenShops,
        markerImages: Map<String, MapMarkerImage>,
    ) {
        val nextShops = shops.values.associate { it.id to ArchiveShopKey(it) }
        val removed =
            renderedShops
                .filter { (id, key) ->
                    nextShops[id] != key || this.markerImages[id] != markerImages[id]
                }.values
        val added =
            nextShops
                .filter { (id, key) ->
                    renderedShops[id] != key || this.markerImages[id] != markerImages[id]
                }.values
        val isMapChanged = this.naverMap !== naverMap
        this.markerImages = markerImages
        this.naverMap = naverMap
        renderedShops = nextShops
        if (removed.isNotEmpty()) clusterer.removeAll(removed)
        if (added.isNotEmpty()) clusterer.addAll(added.associateWith { it.shop })
        if (isMapChanged) clusterer.map = naverMap
    }

    fun dispose() {
        naverMap = null
        markerImages = emptyMap()
        renderedShops = emptyMap()
        clusterer.map = null
        clusterer.clear()
    }
}
