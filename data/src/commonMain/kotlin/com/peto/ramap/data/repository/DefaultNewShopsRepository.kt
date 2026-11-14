package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.shop.NewShopsDataSource
import com.peto.ramap.data.model.NewShopResponse
import com.peto.ramap.domain.model.shop.NewShop
import com.peto.ramap.domain.model.shop.ShopOpeningMonth
import com.peto.ramap.domain.repository.NewShopsRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultNewShopsRepository(
    private val dataSource: NewShopsDataSource,
) : NewShopsRepository {
    override suspend fun fetchNewShops(month: ShopOpeningMonth): RamapResult<List<NewShop>> = invokeRequest { dataSource.fetchNewShops(month).map(NewShopResponse::toDomain) }
}
