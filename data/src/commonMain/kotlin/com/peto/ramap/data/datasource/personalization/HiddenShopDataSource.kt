package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.PersonalizationResponse

interface HiddenShopDataSource {
    suspend fun fetchHiddenShopIds(): List<PersonalizationResponse>

    suspend fun hideShop(shopId: String)

    suspend fun hideBookmarkedShop(shopId: String)

    suspend fun unhideShop(shopId: String)
}
