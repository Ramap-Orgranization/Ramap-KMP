@file:OptIn(ExperimentalForeignApi::class)

package com.peto.ramap.ui.main.map.archive

import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.ui.main.map.archive.nmaps.NMCClusteringKeyProtocol
import com.peto.ramap.ui.main.map.archive.nmaps.NMGLatLng
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSZone
import platform.darwin.NSObject

internal class ArchiveShopClusteringKey(
    val shop: RamenShop,
) : NSObject(),
    NMCClusteringKeyProtocol,
    NSCopyingProtocol {
    override fun position(): NMGLatLng = NMGLatLng.latLngWithLat(shop.location.lat, shop.location.lng)

    override fun copyWithZone(zone: CPointer<NSZone>?): Any = ArchiveShopClusteringKey(shop)

    override fun isEqual(`object`: Any?): Boolean = `object` is ArchiveShopClusteringKey && shop.id == `object`.shop.id

    override fun hash(): ULong =
        shop.id
            .hashCode()
            .toUInt()
            .toULong()
}
