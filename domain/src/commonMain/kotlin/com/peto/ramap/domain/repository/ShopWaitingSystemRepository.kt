package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.WaitingSystem

interface ShopWaitingSystemRepository {
    suspend fun fetchShopWaitingSystem(shopId: String): RamapResult<WaitingSystem?>
}
