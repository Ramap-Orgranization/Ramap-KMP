package com.peto.ramap.data.datasource.personalization

internal interface BookmarkShopDataSource {
    suspend fun fetchBookmarkedShopIds(accountId: String): Set<String>

    suspend fun addBookmarks(
        shopIds: Set<String>,
        accountId: String,
    )

    suspend fun removeBookmark(
        shopId: String,
        accountId: String,
    )
}
