package com.peto.ramap.ui.main.newshops

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import com.peto.ramap.domain.repository.NewShopsRepository

internal class FakeNewShopsRepository(
    private val result: suspend (ShopOpeningMonth) -> RamapResult<List<NewShop>>,
) : NewShopsRepository {
    override suspend fun fetchNewShops(month: ShopOpeningMonth): RamapResult<List<NewShop>> = result(month)
}
