package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakePersonalizationRepository(
    personalization: ShopPersonalization = ShopPersonalization(),
    bootstrapState: PersonalizationBootstrapState = PersonalizationBootstrapState.Ready,
) : ShopPersonalizationStore {
    private val mutableState = MutableStateFlow(personalization)
    override val state = mutableState.asStateFlow()
    private val mutableBootstrapState = MutableStateFlow(bootstrapState)
    override val bootstrapState = mutableBootstrapState.asStateFlow()
    val bookmarkedShopIds = MutableStateFlow(personalization.bookmarkedShopIds)
    val bookmarkUpdateRequests = mutableListOf<Pair<String, Boolean>>()
    var bookmarkUpdateError: RamapError? = null
    var shopNotificationError: RamapError? = null

    override suspend fun refresh(): RamapResult<Unit> {
        mutableBootstrapState.value = PersonalizationBootstrapState.Ready
        return RamapResult.Success(Unit)
    }

    override suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        bookmarkUpdateRequests += shopId to enabled
        bookmarkUpdateError?.let { return RamapResult.Error(it) }
        mutableState.update {
            it.copy(
                bookmarkedShopIds =
                    if (enabled) it.bookmarkedShopIds + shopId else it.bookmarkedShopIds - shopId,
            )
        }
        bookmarkedShopIds.value = mutableState.value.bookmarkedShopIds
        return RamapResult.Success(Unit)
    }

    override suspend fun hideShop(shopId: String): RamapResult<Unit> {
        mutableState.update {
            it.copy(
                bookmarkedShopIds = it.bookmarkedShopIds - shopId,
                hiddenShopIds = it.hiddenShopIds + shopId,
                notificationShopIds = it.notificationShopIds - shopId,
            )
        }
        return RamapResult.Success(Unit)
    }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> {
        mutableState.update { it.copy(hiddenShopIds = it.hiddenShopIds - shopId) }
        return RamapResult.Success(Unit)
    }

    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        shopNotificationError?.let { return RamapResult.Error(it) }
        mutableState.update {
            it.copy(
                notificationShopIds =
                    if (enabled) it.notificationShopIds + shopId else it.notificationShopIds - shopId,
            )
        }
        return RamapResult.Success(Unit)
    }

    override suspend fun clear() {
        mutableState.value = ShopPersonalization()
        mutableBootstrapState.value = PersonalizationBootstrapState.Ready
    }

    fun updateBookmarkedShopIds(shopIds: Set<String>) {
        mutableState.update { it.copy(bookmarkedShopIds = shopIds.toSet()) }
        bookmarkedShopIds.value = shopIds
    }

    suspend fun fetchPersonalization(): RamapResult<ShopPersonalization> = RamapResult.Success(state.value)

    suspend fun removeBookmark(shopId: String): RamapResult<Unit> = updateBookmark(shopId, false)
}
