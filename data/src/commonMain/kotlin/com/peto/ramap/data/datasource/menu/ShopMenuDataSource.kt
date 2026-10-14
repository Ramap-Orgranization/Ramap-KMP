package com.peto.ramap.data.datasource.menu

import com.peto.ramap.data.model.ShopMenuResponse

internal interface ShopMenuDataSource {
    suspend fun fetchShopMenu(shopId: String): ShopMenuResponse
}
