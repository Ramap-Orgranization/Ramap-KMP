package com.peto.ramap.ui.review.search

import com.peto.ramap.platform.storage.SearchHistoryStorage
import kotlinx.coroutines.flow.MutableStateFlow

internal class FakeReviewSearchHistory(
    ids: List<String> = emptyList(),
) : SearchHistoryStorage {
    override val recentSearches = MutableStateFlow(emptyList<String>())
    override val recentlyViewedShopIds = MutableStateFlow(ids)

    override suspend fun addRecentSearch(query: String) = Unit

    override suspend fun removeRecentSearch(query: String) = Unit

    override suspend fun clearRecentSearches() = Unit

    override suspend fun addRecentlyViewedShop(shopId: String) {
        recentlyViewedShopIds.value = listOf(shopId) + recentlyViewedShopIds.value.filterNot { it == shopId }
    }
}
