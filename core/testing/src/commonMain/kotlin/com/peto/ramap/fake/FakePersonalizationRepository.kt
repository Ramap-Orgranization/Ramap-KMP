package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.store.PersonalizationBootstrapState
import com.peto.ramap.domain.store.PersonalizationStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePersonalizationRepository(
    personalization: Personalization = Personalization(),
    initialState: PersonalizationBootstrapState = PersonalizationBootstrapState.Success(personalization),
) : PersonalizationStore {
    private val mutableState = MutableStateFlow(initialState)
    override val state = mutableState.asStateFlow()
    val bookmarkedShopIds = MutableStateFlow(personalization.bookmarkedShopIds)
    val bookmarkUpdateRequests = mutableListOf<Pair<String, Boolean>>()
    val bookmarkBulkAddRequests = mutableListOf<Set<String>>()
    var bookmarkUpdateError: RamapError? = null
    var shopNotificationError: RamapError? = null

    override suspend fun synchronizeGuestBookmarks(accountId: String): RamapResult<Unit> = RamapResult.Success(Unit)

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

    override suspend fun addBookmarks(shopIds: Set<String>): RamapResult<Unit> {
        bookmarkBulkAddRequests += shopIds
        bookmarkUpdateError?.let { return RamapResult.Error(it) }
        publish(currentPersonalization.addBookmarks(shopIds))
        bookmarkedShopIds.value = currentPersonalization.bookmarkedShopIds
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

    override fun publishNotificationSettings(settings: NotificationSettings): Boolean {
        publish(currentPersonalization.changeNotificationSubscription(settings.shopId, settings.hasEffectiveSubscription))
        return true
    }

    override suspend fun clearShopNotifications(shopId: String): RamapResult<Unit> = updateShopNotification(shopId, false)

    override suspend fun clear() {
        publish(Personalization())
    }

    fun updateBootstrapState(state: PersonalizationBootstrapState) {
        mutableState.value = state
    }

    fun updateBookmarkedShopIds(shopIds: Set<String>) {
        publish(currentPersonalization.copy(bookmarkedShopIds = shopIds.toSet()))
        bookmarkedShopIds.value = shopIds
    }

    suspend fun fetchPersonalization(): RamapResult<Personalization> = RamapResult.Success(currentPersonalization)

    suspend fun removeBookmark(shopId: String): RamapResult<Unit> = updateBookmark(shopId, false)

    private val currentPersonalization: Personalization
        get() = (state.value as? PersonalizationBootstrapState.Success)?.value ?: Personalization()

    private fun publish(personalization: Personalization) {
        mutableState.value = PersonalizationBootstrapState.Success(personalization)
    }
}
