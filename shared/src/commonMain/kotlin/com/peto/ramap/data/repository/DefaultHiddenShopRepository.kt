package com.peto.ramap.data.repository

import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.domain.repository.HiddenShopRepository

class DefaultHiddenShopRepository(
    private val dataSource: HiddenShopDataSource,
) : HiddenShopRepository {
    override suspend fun fetchHiddenShopIds(): Set<String> = dataSource.fetchHiddenShopIds().map { it.shopId }.toSet()

    override suspend fun hideShop(shopId: String) {
        dataSource.hideShop(shopId)
    }

    override suspend fun unhideShop(shopId: String) {
        dataSource.unhideShop(shopId)
    }
}
