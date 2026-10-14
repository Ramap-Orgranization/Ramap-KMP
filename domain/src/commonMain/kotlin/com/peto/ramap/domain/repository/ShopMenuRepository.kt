package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.menu.ShopMenu

interface ShopMenuRepository {
    suspend fun fetchShopMenu(shopId: String): RamapResult<ShopMenu>
}
