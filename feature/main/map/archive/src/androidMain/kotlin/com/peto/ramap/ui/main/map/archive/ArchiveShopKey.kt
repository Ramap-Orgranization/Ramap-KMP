package com.peto.ramap.ui.main.map.archive

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusteringKey
import com.peto.ramap.domain.model.shop.RamenShop

internal data class ArchiveShopKey(
    val shop: RamenShop,
) : ClusteringKey {
    override fun getPosition() = LatLng(shop.location.lat, shop.location.lng)
}
