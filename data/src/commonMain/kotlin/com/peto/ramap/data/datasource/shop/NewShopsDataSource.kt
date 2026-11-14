package com.peto.ramap.data.datasource.shop

import com.peto.ramap.data.model.NewShopResponse
import com.peto.ramap.domain.model.shop.ShopRegistrationMonth

internal interface NewShopsDataSource {
    suspend fun fetchNewShops(month: ShopRegistrationMonth): List<NewShopResponse>
}
