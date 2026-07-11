package com.peto.ramap.ui.map.model

import com.naver.maps.geometry.LatLng
import com.naver.maps.map.clustering.ClusteringKey
import com.peto.ramap.domain.model.RamenShop

internal data class ShopKey(
    val shop: RamenShop,
) : ClusteringKey {
    override fun getPosition() = LatLng(shop.location.lat, shop.location.lng)
}
