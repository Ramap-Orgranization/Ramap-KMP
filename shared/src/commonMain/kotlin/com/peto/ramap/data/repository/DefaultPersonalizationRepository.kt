package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.Personalization
import com.peto.ramap.domain.repository.BookmarkShopRepository
import com.peto.ramap.domain.repository.HiddenShopRepository
import com.peto.ramap.domain.repository.PersonalizationRepository
import com.peto.ramap.network.execute.invokeRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DefaultPersonalizationRepository(
    private val bookmarkShopRepository: BookmarkShopRepository,
    private val hiddenShopRepository: HiddenShopRepository,
) : PersonalizationRepository {
    override suspend fun fetchPersonalization(): RamapResult<Personalization> =
        invokeRequest {
            coroutineScope {
                val bookmarks =
                    async {
                        bookmarkShopRepository.fetchBookmarkedShopIds()
                    }
                val hiddenShops =
                    async {
                        hiddenShopRepository.fetchHiddenShopIds()
                    }

                Personalization(
                    bookmarkedShopIds = bookmarks.await(),
                    hiddenShopIds = hiddenShops.await(),
                )
            }
        }

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> = invokeRequest { bookmarkShopRepository.addBookmark(shopId) }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> = invokeRequest { bookmarkShopRepository.removeBookmark(shopId) }

    override suspend fun hideShop(shopId: String): RamapResult<Unit> = invokeRequest { hiddenShopRepository.hideShop(shopId) }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> = invokeRequest { hiddenShopRepository.unhideShop(shopId) }
}
