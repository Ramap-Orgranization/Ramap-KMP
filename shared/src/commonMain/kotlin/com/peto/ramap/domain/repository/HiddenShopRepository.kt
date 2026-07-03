package com.peto.ramap.domain.repository

interface HiddenShopRepository {
    suspend fun fetchHiddenShopIds(): Set<String>

    suspend fun hideShop(shopId: String)

    suspend fun unhideShop(shopId: String)
}
