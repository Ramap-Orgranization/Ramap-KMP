package com.peto.ramap.ui.map

import android.graphics.Bitmap
import com.naver.maps.map.NaverMap
import com.naver.maps.map.clustering.Clusterer
import com.naver.maps.map.clustering.DefaultLeafMarkerUpdater
import com.naver.maps.map.clustering.LeafMarkerInfo
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import com.peto.ramap.core.config.MapInteractionConfig
import com.peto.ramap.domain.model.RamenShop
import com.peto.ramap.ui.map.model.ShopKey

internal class NaverShopClusterRenderer(
    markerBitmap: Bitmap,
    onShopClick: (RamenShop) -> Unit,
) {
    private val clusterer =
        Clusterer
            .Builder<ShopKey>()
            .maxZoom(MapInteractionConfig.CLUSTER_MAX_ZOOM_LEVEL)
            .leafMarkerUpdater(
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
        shops: List<RamenShop>,
    ) {
        clusterer.map = null
        clusterer.clear()
        clusterer.addAll(shops.associateBy { ShopKey(it) })
        clusterer.map = naverMap
    }

    fun dispose() {
        clusterer.map = null
        clusterer.clear()
    }
}
