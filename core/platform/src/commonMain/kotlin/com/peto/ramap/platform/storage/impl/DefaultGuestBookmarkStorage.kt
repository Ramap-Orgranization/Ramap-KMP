package com.peto.ramap.platform.storage.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.peto.ramap.platform.storage.GuestBookmarkStorage
import kotlinx.coroutines.flow.first

class DefaultGuestBookmarkStorage(
    private val dataStore: DataStore<Preferences>,
) : GuestBookmarkStorage {
    override suspend fun fetchGuestBookmarks(): Set<String> = dataStore.data.first()[GUEST_BOOKMARKS].orEmpty()

    override suspend fun addGuestBookmarks(shopIds: Set<String>) {
        dataStore.edit { it[GUEST_BOOKMARKS] = it[GUEST_BOOKMARKS].orEmpty() + shopIds }
    }

    override suspend fun removeGuestBookmark(shopId: String) {
        dataStore.edit { it[GUEST_BOOKMARKS] = it[GUEST_BOOKMARKS].orEmpty() - shopId }
    }

    override suspend fun claimGuestBookmarks(accountId: String) {
        val pending = pendingKey(accountId)
        dataStore.edit {
            val guestBookmarks = it[GUEST_BOOKMARKS].orEmpty()
            if (guestBookmarks.isNotEmpty()) {
                it[pending] = it[pending].orEmpty() + guestBookmarks
                it.remove(GUEST_BOOKMARKS)
            }
        }
    }

    override suspend fun fetchPendingBookmarks(accountId: String): Set<String> = dataStore.data.first()[pendingKey(accountId)].orEmpty()

    override suspend fun forgetPendingBookmarks(
        accountId: String,
        shopIds: Set<String>,
    ) {
        val pending = pendingKey(accountId)
        dataStore.edit {
            val remaining = it[pending].orEmpty() - shopIds
            if (remaining.isEmpty()) it.remove(pending) else it[pending] = remaining
        }
    }

    private fun pendingKey(accountId: String) = stringSetPreferencesKey("$PENDING_GUEST_BOOKMARKS_PREFIX$accountId")

    private companion object {
        val GUEST_BOOKMARKS = stringSetPreferencesKey("guest_bookmarks")
        private const val PENDING_GUEST_BOOKMARKS_PREFIX = "pending_guest_bookmarks_"
    }
}
