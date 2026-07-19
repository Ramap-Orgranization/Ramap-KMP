package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult

interface HiddenShopRepository {
    suspend fun fetchHiddenShopIds(): RamapResult<Set<String>>

    suspend fun hideShop(shopId: String): RamapResult<Unit>

    suspend fun hideBookmarkedShop(shopId: String): RamapResult<Unit>

    suspend fun unhideShop(shopId: String): RamapResult<Unit>
}
