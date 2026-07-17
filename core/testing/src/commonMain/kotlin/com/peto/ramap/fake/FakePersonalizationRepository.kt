package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.repository.PersonalizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakePersonalizationRepository(
    personalization: Personalization = Personalization(),
) : PersonalizationRepository {
    private val mutableBookmarkedShopIds = MutableStateFlow(personalization.bookmarkedShopIds.toSet())
    private val hiddenShopIds = personalization.hiddenShopIds.toMutableSet()
    override val bookmarkedShopIds = mutableBookmarkedShopIds.asStateFlow()

    override suspend fun fetchPersonalization(): RamapResult<Personalization> =
        RamapResult.Success(
            Personalization(
                bookmarkedShopIds = bookmarkedShopIds.value,
                hiddenShopIds = hiddenShopIds.toSet(),
            ),
        )

    override suspend fun addBookmark(shopId: String): RamapResult<Unit> {
        mutableBookmarkedShopIds.update { it + shopId }
        return RamapResult.Success(Unit)
    }

    override suspend fun removeBookmark(shopId: String): RamapResult<Unit> {
        mutableBookmarkedShopIds.update { it - shopId }
        return RamapResult.Success(Unit)
    }

    override suspend fun hideShop(
        shopId: String,
        removeBookmark: Boolean,
    ): RamapResult<Unit> {
        hiddenShopIds += shopId
        if (removeBookmark) {
            mutableBookmarkedShopIds.update { it - shopId }
        }
        return RamapResult.Success(Unit)
    }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> {
        hiddenShopIds -= shopId
        return RamapResult.Success(Unit)
    }

    fun updateBookmarkedShopIds(shopIds: Set<String>) {
        mutableBookmarkedShopIds.value = shopIds.toSet()
    }
}
