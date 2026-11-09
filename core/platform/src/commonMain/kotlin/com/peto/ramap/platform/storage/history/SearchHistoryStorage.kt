package com.peto.ramap.platform.storage.history

import kotlinx.coroutines.flow.StateFlow

interface SearchHistoryStorage {
    val recentSearches: StateFlow<List<String>>
    val recentlyViewedShopIds: StateFlow<List<String>>

    suspend fun addRecentSearch(query: String)

    suspend fun removeRecentSearch(query: String)

    suspend fun clearRecentSearches()

    suspend fun addRecentlyViewedShop(shopId: String)
}
