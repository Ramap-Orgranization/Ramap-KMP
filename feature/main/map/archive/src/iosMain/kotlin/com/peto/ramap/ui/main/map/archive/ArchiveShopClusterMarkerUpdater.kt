@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.archive.model.hasOnlyOverlappingMarkers
import com.peto.ramap.ui.main.map.archive.nmaps.NMCClusterMarkerInfo
import com.peto.ramap.ui.main.map.archive.nmaps.NMCDefaultClusterMarkerUpdater
import com.peto.ramap.ui.main.map.archive.nmaps.NMFMarker
import com.peto.ramap.ui.main.map.archive.nmaps.NMGLatLng
import kotlinx.cinterop.ExperimentalForeignApi

internal class ArchiveShopClusterMarkerUpdater(
    private val onClusterClick: (List<RamenShop>, NMGLatLng) -> Unit,
) : NMCDefaultClusterMarkerUpdater() {
    override fun updateClusterMarker(
        info: NMCClusterMarkerInfo,
        _1: NMFMarker,
    ) {
        super.updateClusterMarker(info, _1)
        val shops = (info.tag as? ArchiveShopClusterTag)?.shops ?: return
        if (shops.size != info.size.toInt() || !hasOnlyOverlappingMarkers(shops)) return
        _1.touchHandler = {
            onClusterClick(shops, info.position)
            true
        }
    }
}
