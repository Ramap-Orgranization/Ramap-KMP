package com.peto.ramap.ui.main.map.model.cache

import com.peto.ramap.ui.main.map.model.ShopDetail
import com.peto.ramap.ui.main.map.model.ShopId

class ShopDetailCache(
    private val values: MutableMap<ShopId, ShopDetail> = mutableMapOf(),
) {
    fun find(shopId: ShopId): ShopDetailCacheLookup =
        values[shopId]
            ?.let(ShopDetailCacheLookup::Hit)
            ?: ShopDetailCacheLookup.Miss

    fun store(detail: ShopDetail) {
        values[ShopId(detail.shop.id)] = detail
    }
}
