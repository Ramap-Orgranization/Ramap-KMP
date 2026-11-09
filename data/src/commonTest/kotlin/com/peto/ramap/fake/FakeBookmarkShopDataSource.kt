package com.peto.ramap.fake

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource

internal class FakeBookmarkShopDataSource : BookmarkShopDataSource {
    val bookmarksByAccount = mutableMapOf<String, Set<String>>()
    val addRequests = mutableListOf<Pair<String, Set<String>>>()
    var add: suspend (String, Set<String>) -> Unit = ::save

    override suspend fun fetchBookmarkedShopIds(accountId: String): Set<String> = bookmarksByAccount[accountId].orEmpty()

    override suspend fun addBookmarks(
        shopIds: Set<String>,
        accountId: String,
    ) {
        addRequests += accountId to shopIds
        add(accountId, shopIds)
    }

    override suspend fun removeBookmark(
        shopId: String,
        accountId: String,
    ) {
        bookmarksByAccount[accountId] = bookmarksByAccount[accountId].orEmpty() - shopId
    }

    fun save(
        accountId: String,
        shopIds: Set<String>,
    ) {
        bookmarksByAccount[accountId] = bookmarksByAccount[accountId].orEmpty() + shopIds
    }
}
