package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.domain.repository.HiddenShopRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultHiddenShopRepository(
    private val dataSource: HiddenShopDataSource,
) : HiddenShopRepository {
    override suspend fun fetchHiddenShopIds(): RamapResult<Set<String>> =
        invokeRequest { dataSource.fetchHiddenShopIds().mapTo(mutableSetOf()) { it.shopId } }

    override suspend fun hideShop(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.hideShop(shopId) }

    override suspend fun hideBookmarkedShop(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.hideBookmarkedShop(shopId) }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.unhideShop(shopId) }
}
