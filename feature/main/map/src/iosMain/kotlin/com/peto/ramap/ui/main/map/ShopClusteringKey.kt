@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map

import cocoapods.NMapsMap.NMCClusteringKeyProtocol
import cocoapods.NMapsMap.NMGLatLng
import com.peto.ramap.domain.model.shop.RamenShop
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSZone
import platform.darwin.NSObject

internal class ShopClusteringKey(
    val shop: RamenShop,
) : NSObject(),
    NMCClusteringKeyProtocol,
    NSCopyingProtocol {
    override fun position(): NMGLatLng = NMGLatLng.latLngWithLat(shop.location.lat, shop.location.lng)

    override fun copyWithZone(zone: CPointer<NSZone>?): Any = ShopClusteringKey(shop)

    override fun isEqual(`object`: Any?): Boolean = `object` is ShopClusteringKey && shop.id == `object`.shop.id

    override fun hash(): ULong =
        shop.id
            .hashCode()
            .toUInt()
            .toULong()
}
