package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.personalization.ShopPersonalization
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.ShopPersonalizationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePersonalizationRepository(
    personalization: ShopPersonalization = ShopPersonalization(),
    initialState: PersonalizationBootstrapState = PersonalizationBootstrapState.Success(personalization),
) : ShopPersonalizationStore {
    private val mutableState = MutableStateFlow(initialState)
    override val state = mutableState.asStateFlow()
    val bookmarkedShopIds = MutableStateFlow(personalization.bookmarkedShopIds)
    val bookmarkUpdateRequests = mutableListOf<Pair<String, Boolean>>()
    var bookmarkUpdateError: RamapError? = null
    var shopNotificationError: RamapError? = null

    override suspend fun refresh(): RamapResult<Unit> {
        publish(currentPersonalization)
        return RamapResult.Success(Unit)
    }

    override suspend fun updateBookmark(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        bookmarkUpdateRequests += shopId to enabled
        bookmarkUpdateError?.let { return RamapResult.Error(it) }
        publish(
            currentPersonalization.let {
                it.copy(
                    bookmarkedShopIds =
                        if (enabled) it.bookmarkedShopIds + shopId else it.bookmarkedShopIds - shopId,
                )
            },
        )
        bookmarkedShopIds.value = currentPersonalization.bookmarkedShopIds
        return RamapResult.Success(Unit)
    }

    override suspend fun hideShop(shopId: String): RamapResult<Unit> {
        publish(
            currentPersonalization.let {
                it.copy(
                    bookmarkedShopIds = it.bookmarkedShopIds - shopId,
                    hiddenShopIds = it.hiddenShopIds + shopId,
                    notificationShopIds = it.notificationShopIds - shopId,
                )
            },
        )
        return RamapResult.Success(Unit)
    }

    override suspend fun unhideShop(shopId: String): RamapResult<Unit> {
        publish(currentPersonalization.copy(hiddenShopIds = currentPersonalization.hiddenShopIds - shopId))
        return RamapResult.Success(Unit)
    }

    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        shopNotificationError?.let { return RamapResult.Error(it) }
        publish(
            currentPersonalization.let {
                it.copy(
                    notificationShopIds =
                        if (enabled) it.notificationShopIds + shopId else it.notificationShopIds - shopId,
                )
            },
        )
        return RamapResult.Success(Unit)
    }

    override suspend fun clear() {
        publish(ShopPersonalization())
    }

    fun updateBookmarkedShopIds(shopIds: Set<String>) {
        publish(currentPersonalization.copy(bookmarkedShopIds = shopIds.toSet()))
        bookmarkedShopIds.value = shopIds
    }

    suspend fun fetchPersonalization(): RamapResult<ShopPersonalization> = RamapResult.Success(currentPersonalization)

    suspend fun removeBookmark(shopId: String): RamapResult<Unit> = updateBookmark(shopId, false)

    private val currentPersonalization: ShopPersonalization
        get() = (state.value as? PersonalizationBootstrapState.Success)?.value ?: ShopPersonalization()

    private fun publish(personalization: ShopPersonalization) {
        mutableState.value = PersonalizationBootstrapState.Success(personalization)
    }
}
