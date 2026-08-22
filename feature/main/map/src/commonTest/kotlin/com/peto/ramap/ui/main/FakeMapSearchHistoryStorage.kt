package com.peto.ramap.ui.main

import com.peto.ramap.domain.storage.SearchHistoryStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeMapSearchHistoryStorage : SearchHistoryStorage {
    private val searches = MutableStateFlow(emptyList<String>())
    private val viewedShopIds = MutableStateFlow(emptyList<String>())

    override val recentSearches: StateFlow<List<String>> = searches
    override val recentlyViewedShopIds: StateFlow<List<String>> = viewedShopIds

    override suspend fun addRecentSearch(query: String) {
        searches.value = listOf(query) + searches.value.filterNot { it == query }.take(9)
    }

    override suspend fun removeRecentSearch(query: String) {
        searches.value = searches.value.filterNot { it == query }
    }

    override suspend fun clearRecentSearches() {
        searches.value = emptyList()
    }

    override suspend fun addRecentlyViewedShop(shopId: String) {
        viewedShopIds.value = listOf(shopId) + viewedShopIds.value.filterNot { it == shopId }.take(9)
    }
}
