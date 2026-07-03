package com.peto.ramap.fake

import com.peto.ramap.data.datasource.personalization.BookmarkShopDataSource
import com.peto.ramap.data.model.PersonalizationResponse

class FakeBookmarkShopDataSource(
    initialBookmarks: List<PersonalizationResponse> = emptyList(),
) : BookmarkShopDataSource {
    private val bookmarks = initialBookmarks.toMutableList()

    override suspend fun fetchBookmarkedShopIds(): List<PersonalizationResponse> = bookmarks.toList()

    override suspend fun addBookmark(shopId: String) {
        if (bookmarks.none { it.shopId == shopId }) {
            bookmarks += PersonalizationResponse(shopId)
        }
    }

    override suspend fun removeBookmark(shopId: String) {
        bookmarks.removeAll { it.shopId == shopId }
    }
}
