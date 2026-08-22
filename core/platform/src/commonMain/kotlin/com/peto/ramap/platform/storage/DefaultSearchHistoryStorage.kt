package com.peto.ramap.platform.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.peto.ramap.domain.storage.SearchHistoryStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DefaultSearchHistoryStorage(
    private val dataStore: DataStore<Preferences>,
) : SearchHistoryStorage {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val recentSearches: StateFlow<List<String>> =
        dataStore.data.map { it[RECENT_SEARCHES_KEY].decode() }.stateIn(
            scope,
            SharingStarted.Eagerly,
            emptyList(),
        )
    override val recentlyViewedShopIds: StateFlow<List<String>> =
        dataStore.data.map { it[RECENTLY_VIEWED_SHOPS_KEY].decode() }.stateIn(
            scope,
            SharingStarted.Eagerly,
            emptyList(),
        )

    override suspend fun addRecentSearch(query: String) = update(RECENT_SEARCHES_KEY, query)

    override suspend fun removeRecentSearch(query: String) {
        dataStore.edit { preferences ->
            preferences[RECENT_SEARCHES_KEY] =
                preferences[RECENT_SEARCHES_KEY].decode().filterNot { it == query }.encode()
        }
    }

    override suspend fun clearRecentSearches() {
        dataStore.edit { it.remove(RECENT_SEARCHES_KEY) }
    }

    override suspend fun addRecentlyViewedShop(shopId: String) = update(RECENTLY_VIEWED_SHOPS_KEY, shopId)

    private suspend fun update(
        key: Preferences.Key<String>,
        value: String,
    ) {
        if (value.isBlank()) return
        dataStore.edit { preferences ->
            val currentHistory = preferences[key].decode()
            val remainingHistory =
                currentHistory.filterNot { it == value }.take(MAX_HISTORY_SIZE - 1)
            val updatedHistory = listOf(value) + remainingHistory

            preferences[key] = updatedHistory.encode()
        }
    }

    private fun String?.decode(): List<String> = this?.split(SEPARATOR)?.filter(String::isNotBlank).orEmpty()

    private fun List<String>.encode(): String = take(MAX_HISTORY_SIZE).joinToString(SEPARATOR)

    private companion object {
        const val MAX_HISTORY_SIZE = 10
        const val SEPARATOR = "\u001F"
        val RECENT_SEARCHES_KEY = stringPreferencesKey("recent_searches")
        val RECENTLY_VIEWED_SHOPS_KEY = stringPreferencesKey("recently_viewed_shops")
    }
}
