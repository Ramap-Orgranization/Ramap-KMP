package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.NewShopResponse
import com.peto.ramap.domain.model.shop.ShopOpeningMonth

internal interface NewShopsDataSource {
    suspend fun fetchNewShops(month: ShopOpeningMonth): List<NewShopResponse>
}
