@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main

import cocoapods.NMapsMap.NMCClusterMarkerInfo
import cocoapods.NMapsMap.NMCDefaultClusterMarkerUpdater
import cocoapods.NMapsMap.NMFMarker
import cocoapods.NMapsMap.NMGLatLng
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.hasOnlyOverlappingMarkers
import kotlinx.cinterop.ExperimentalForeignApi

internal class ShopClusterMarkerUpdater(
    private val onClusterClick: (List<RamenShop>, NMGLatLng) -> Unit,
) : NMCDefaultClusterMarkerUpdater() {
    override fun updateClusterMarker(
        info: NMCClusterMarkerInfo,
        _1: NMFMarker,
    ) {
        super.updateClusterMarker(info, _1)
        val shops = (info.tag as? ShopClusterTag)?.shops ?: return
        if (shops.size != info.size.toInt() || !hasOnlyOverlappingMarkers(shops)) return
        _1.touchHandler = {
            onClusterClick(shops, info.position)
            true
        }
    }
}
