package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.menu.ShopMenuDataSource
import com.peto.ramap.domain.model.menu.ShopMenu
import com.peto.ramap.domain.repository.ShopMenuRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultShopMenuRepository(
    private val dataSource: ShopMenuDataSource,
) : ShopMenuRepository {
    override suspend fun fetchShopMenu(shopId: String): RamapResult<ShopMenu> = invokeRequest {
        dataSource.fetchShopMenu(shopId).toDomain()
    }
}
