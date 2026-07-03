package com.peto.ramap.fake

import com.peto.ramap.domain.model.Personalization
import com.peto.ramap.domain.repository.PersonalizationRepository

class FakePersonalizationRepository(
    personalization: Personalization = Personalization(),
) : PersonalizationRepository {
    private val bookmarkedShopIds = personalization.bookmarkedShopIds.toMutableSet()
    private val hiddenShopIds = personalization.hiddenShopIds.toMutableSet()

    override suspend fun fetchPersonalization(): Personalization =
        Personalization(
            bookmarkedShopIds = bookmarkedShopIds.toSet(),
            hiddenShopIds = hiddenShopIds.toSet(),
        )

    override suspend fun addBookmark(shopId: String) {
        bookmarkedShopIds += shopId
    }

    override suspend fun removeBookmark(shopId: String) {
        bookmarkedShopIds -= shopId
    }

    override suspend fun hideShop(shopId: String) {
        hiddenShopIds += shopId
    }

    override suspend fun unhideShop(shopId: String) {
        hiddenShopIds -= shopId
    }
}
