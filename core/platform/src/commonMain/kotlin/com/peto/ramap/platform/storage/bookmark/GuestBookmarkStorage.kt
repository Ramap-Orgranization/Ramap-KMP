package com.peto.ramap.platform.storage.bookmark

interface GuestBookmarkStorage {
    suspend fun fetchGuestBookmarks(): Set<String>

    suspend fun addGuestBookmarks(shopIds: Set<String>)

    suspend fun removeGuestBookmark(shopId: String)

    suspend fun claimGuestBookmarks(accountId: String)

    suspend fun fetchPendingBookmarks(accountId: String): Set<String>

    suspend fun forgetPendingBookmarks(
        accountId: String,
        shopIds: Set<String>,
    )
}
