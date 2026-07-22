package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.shop.ShopRankings

interface ShopRankingRepository {
    suspend fun fetchShopRankings(): RamapResult<ShopRankings>
}
