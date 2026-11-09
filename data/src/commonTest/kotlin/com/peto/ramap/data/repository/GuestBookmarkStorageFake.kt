package com.peto.ramap.data.repository

import com.peto.ramap.platform.storage.GuestBookmarkStorage

internal class GuestBookmarkStorageFake : GuestBookmarkStorage {
    private var guestBookmarks = emptySet<String>()
    private val pendingByAccount = mutableMapOf<String, Set<String>>()
    var beforeAdd: suspend () -> Unit = {}
    var beforeForget: suspend () -> Unit = {}

    override suspend fun fetchGuestBookmarks(): Set<String> = guestBookmarks

    override suspend fun addGuestBookmarks(shopIds: Set<String>) {
        beforeAdd()
        guestBookmarks += shopIds
    }

    override suspend fun removeGuestBookmark(shopId: String) {
        guestBookmarks -= shopId
    }

    override suspend fun claimGuestBookmarks(accountId: String) {
        val pending = pendingByAccount[accountId].orEmpty() + guestBookmarks
        pendingByAccount[accountId] = pending
        guestBookmarks = emptySet()
    }

    override suspend fun fetchPendingBookmarks(accountId: String): Set<String> = pendingByAccount[accountId].orEmpty()

    override suspend fun forgetPendingBookmarks(
        accountId: String,
        shopIds: Set<String>,
    ) {
        beforeForget()
        pendingByAccount[accountId] = pendingByAccount[accountId].orEmpty() - shopIds
    }
}
