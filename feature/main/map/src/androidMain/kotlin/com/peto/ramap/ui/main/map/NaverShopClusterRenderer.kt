package com.peto.ramap.ui.main.map

import android.graphics.Bitmap
import com.naver.maps.map.NaverMap
import com.naver.maps.map.clustering.ClusterMarkerInfo
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.DefaultClusterMarkerUpdater
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.RamenShops
import com.peto.ramap.ui.main.map.config.MapInteractionConfig
import com.peto.ramap.ui.main.map.model.ShopKey

internal class NaverShopClusterRenderer(
    markerBitmap: Bitmap,
    onShopClick: (RamenShop) -> Unit,
    onClusterClick: (List<RamenShop>, Int, Int) -> Unit,
) {
    private var naverMap: NaverMap? = null
    private val clusterer =
        Clusterer
            .ComplexBuilder<ShopKey>()
            .maxClusteringZoom(MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL)
            .maxIndexingZoom(MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL)
            .animationDuration(0)
            .tagMergeStrategy(ShopTagMergeStrategy())
            .clusterMarkerUpdater(
                object : DefaultClusterMarkerUpdater() {
                    override fun updateClusterMarker(
                        info: ClusterMarkerInfo,
                        marker: Marker,
                    ) {
                        super.updateClusterMarker(info, marker)
                        val shops = info.tag as? List<*> ?: return
                        val clusterShops = shops.filterIsInstance<RamenShop>()
                        if (clusterShops.size != info.size || !hasOnlyOverlappingMarkers(clusterShops)) return
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
                        val shop = (info.key as ShopKey).shop
                        marker.icon = OverlayImage.fromBitmap(markerBitmap)
                        marker.captionText = shop.name
                        marker.alpha = if (shop.isVisible) 1.0f else MapInteractionConfig.HIDDEN_SHOP_ALPHA
                        marker.onClickListener =
                            Overlay.OnClickListener {
                                onShopClick(shop)
                                true
                            }
                    }
                },
            ).build()

    fun render(
        naverMap: NaverMap,
        shops: RamenShops,
    ) {
        this.naverMap = naverMap
        clusterer.map = null
        clusterer.clear()
        clusterer.addAll(shops.values.associateBy { ShopKey(it) })
        clusterer.map = naverMap
    }

    fun dispose() {
        naverMap = null
        clusterer.map = null
        clusterer.clear()
    }
}
