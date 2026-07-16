package com.peto.ramap.fake

import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.model.PersonalizationResponse

class FakeHiddenShopDataSource(
    initialHiddenShops: List<PersonalizationResponse> = emptyList(),
) : HiddenShopDataSource {
    private val hiddenShops = initialHiddenShops.toMutableList()
    var hiddenBookmarkedShopId: String? = null
        private set

    override suspend fun fetchHiddenShopIds(): List<PersonalizationResponse> = hiddenShops.toList()

    override suspend fun hideShop(shopId: String) {
        if (hiddenShops.none { it.shopId == shopId }) {
            hiddenShops += PersonalizationResponse(shopId)
        }
    }

    override suspend fun hideBookmarkedShop(shopId: String) {
        hiddenBookmarkedShopId = shopId
        hideShop(shopId)
    }

    override suspend fun unhideShop(shopId: String) {
        hiddenShops.removeAll { it.shopId == shopId }
    }
}
