package com.peto.ramap.fake

import com.peto.ramap.data.datasource.menu.ShopMenuDataSource
import com.peto.ramap.data.model.ShopMenuResponse

internal class FakeShopMenuDataSource(
    private val response: ShopMenuResponse = ShopMenuResponse(),
    private val error: Throwable? = null,
) : ShopMenuDataSource {
    val requestedShopIds = mutableListOf<String>()

    override suspend fun fetchShopMenu(shopId: String): ShopMenuResponse {
        requestedShopIds += shopId
        error?.let { throw it }
        return response
    }
}
