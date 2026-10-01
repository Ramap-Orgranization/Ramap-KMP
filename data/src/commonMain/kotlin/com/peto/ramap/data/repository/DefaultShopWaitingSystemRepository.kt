package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.waiting.ShopWaitingSystemDataSource
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.domain.repository.ShopWaitingSystemRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultShopWaitingSystemRepository(
    private val dataSource: ShopWaitingSystemDataSource,
) : ShopWaitingSystemRepository {
    override suspend fun fetchShopWaitingSystem(shopId: String): RamapResult<WaitingSystem?> = invokeRequest { dataSource.fetchShopWaitingSystem(shopId)?.toDomain(shopId) }
}
