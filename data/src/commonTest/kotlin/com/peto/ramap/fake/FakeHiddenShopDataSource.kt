package com.peto.ramap.fake

import com.peto.ramap.data.datasource.personalization.HiddenShopDataSource
import com.peto.ramap.data.model.PersonalizationResponse

internal class FakeHiddenShopDataSource(
    initialHiddenShops: List<PersonalizationResponse> = emptyList(),
) : HiddenShopDataSource {
    private val hiddenShops = initialHiddenShops.toMutableList()
    var error: Throwable? = null
    var hiddenBookmarkedShopId: String? = null
        private set

    override suspend fun fetchHiddenShopIds(): List<PersonalizationResponse> {
        error?.let { throw it }
        return hiddenShops.toList()
    }

    override suspend fun hideShop(shopId: String) {
        error?.let { throw it }
        if (hiddenShops.none { it.shopId == shopId }) {
            hiddenShops += PersonalizationResponse(shopId)
        }
    }

    override suspend fun hideBookmarkedShop(shopId: String) {
        error?.let { throw it }
        hiddenBookmarkedShopId = shopId
        hideShop(shopId)
    }

    override suspend fun unhideShop(shopId: String) {
        error?.let { throw it }
        hiddenShops.removeAll { it.shopId == shopId }
    }
}
