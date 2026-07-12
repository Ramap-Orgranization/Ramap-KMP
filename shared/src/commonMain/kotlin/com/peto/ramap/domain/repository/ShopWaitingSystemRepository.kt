package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.ShopWaitingSystem

interface ShopWaitingSystemRepository {
    suspend fun fetchShopWaitingSystem(shopId: String): RamapResult<ShopWaitingSystem?>
}
