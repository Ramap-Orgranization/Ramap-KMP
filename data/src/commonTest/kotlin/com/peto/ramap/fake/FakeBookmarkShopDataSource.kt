package com.peto.ramap.fake

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.model.PersonalizationResponse

class FakeBookmarkShopDataSource(
    initialBookmarks: List<PersonalizationResponse> = emptyList(),
) : BookmarkShopDataSource {
    private val bookmarks = initialBookmarks.toMutableList()
    var error: Throwable? = null

    override suspend fun fetchBookmarkedShopIds(): List<PersonalizationResponse> {
        error?.let { throw it }
        return bookmarks.toList()
    }

    override suspend fun addBookmark(shopId: String) {
        error?.let { throw it }
        if (bookmarks.none { it.shopId == shopId }) {
            bookmarks += PersonalizationResponse(shopId)
        }
    }

    override suspend fun removeBookmark(shopId: String) {
        error?.let { throw it }
        bookmarks.removeAll { it.shopId == shopId }
    }
}
