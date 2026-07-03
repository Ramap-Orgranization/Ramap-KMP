package com.peto.ramap.data.repository

import com.peto.ramap.domain.model.Personalization
import com.peto.ramap.domain.repository.BookmarkShopRepository
import com.peto.ramap.domain.repository.HiddenShopRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DefaultPersonalizationRepository(
    private val bookmarkShopRepository: BookmarkShopRepository,
    private val hiddenShopRepository: HiddenShopRepository,
) : PersonalizationRepository {
    override suspend fun fetchPersonalization(): Personalization =
        coroutineScope {
            val bookmarks = async { bookmarkShopRepository.fetchBookmarkedShopIds() }
            val hiddenShops = async { hiddenShopRepository.fetchHiddenShopIds() }

            Personalization(
                bookmarkedShopIds = bookmarks.await(),
                hiddenShopIds = hiddenShops.await(),
            )
        }

    override suspend fun addBookmark(shopId: String) {
        bookmarkShopRepository.addBookmark(shopId)
    }

    override suspend fun removeBookmark(shopId: String) {
        bookmarkShopRepository.removeBookmark(shopId)
    }

    override suspend fun hideShop(shopId: String) {
        hiddenShopRepository.hideShop(shopId)
    }

    override suspend fun unhideShop(shopId: String) {
        hiddenShopRepository.unhideShop(shopId)
    }
}
