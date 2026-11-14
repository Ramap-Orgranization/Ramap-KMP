package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpeningMonth

interface NewShopsRepository {
    suspend fun fetchNewShops(month: ShopOpeningMonth): RamapResult<List<NewShop>>
}
