package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.PersonalizationRepository

class FakePersonalizationRepository(
    personalization: Personalization = Personalization(),
) : PersonalizationRepository {
    private val bookmarkedShopIds = personalization.bookmarkedShopIds.toMutableSet()
    private val hiddenShopIds = personalization.hiddenShopIds.toMutableSet()

    override suspend fun fetchPersonalization(): RamapResult<Personalization> =
        RamapResult.Success(
            Personalization(
                bookmarkedShopIds = bookmarkedShopIds.toSet(),
                hiddenShopIds = hiddenShopIds.toSet(),
            ),
        )

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> {
        bookmarkedShopIds += shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> {
        bookmarkedShopIds -= shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun hideShop(
        shopId: String,
        removeBookmark: Boolean,
    ): RamapResult<Unit> {
        hiddenShopIds += shopId
        if (removeBookmark) bookmarkedShopIds -= shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> {
        hiddenShopIds -= shopId
        return RamapResult.Success(Unit)
    }
}
