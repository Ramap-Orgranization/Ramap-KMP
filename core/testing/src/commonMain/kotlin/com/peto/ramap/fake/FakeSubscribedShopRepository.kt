package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.NotificationCatalog
import com.peto.ramap.domain.model.notification.NotificationSelection
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.repository.SubscribedShopRepository

class FakeSubscribedShopRepository(
    initialShopIds: Set<String> = emptySet(),
    private val shouldFailUpdate: Boolean = false,
) : SubscribedShopRepository {
    val shopIds = initialShopIds.toMutableSet()
    val subscriptionRequests = mutableListOf<String>()
    val unsubscriptionRequests = mutableListOf<String>()
    val saveRequests = mutableListOf<NotificationSettings>()
    val clearAllSaveRequests = mutableListOf<Boolean>()
    val clearRequests = mutableListOf<String>()
    var catalog = NotificationCatalog(1, emptyList())
    val settings = mutableMapOf<String, NotificationSettings>()
    var fetchError: RamapError? = null
    var saveError: RamapError? = null

    override suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>> = RamapResult.Success(shopIds.toSet())

    override suspend fun fetchNotificationSettings(
        shopId: String,
        expectedAccountId: String,
    ): RamapResult<NotificationSettings> {
        fetchError?.let { return RamapResult.Error(it) }
        return RamapResult.Success(settings[shopId] ?: NotificationSettings(expectedAccountId, shopId, catalog, catalog.clear()))
    }

    override suspend fun saveNotificationSettings(
        settings: NotificationSettings,
        selection: NotificationSelection,
        clearAll: Boolean,
    ): RamapResult<NotificationSettings> {
        saveError?.let { return RamapResult.Error(it) }
        val saved =
            settings.copy(
                selection = settings.catalog.selection(selection.ids),
                hasHiddenEffectiveSubscription = !clearAll && settings.hasHiddenEffectiveSubscription,
            )
        clearAllSaveRequests += clearAll
        saveRequests += saved
        this.settings[settings.shopId] = saved
        if (saved.hasEffectiveSubscription) shopIds += settings.shopId else shopIds -= settings.shopId
        return RamapResult.Success(saved)
    }

    override suspend fun clearNotificationSettings(
        shopId: String,
        expectedAccountId: String,
    ): RamapResult<NotificationSettings> {
        clearRequests += shopId
        if (shouldFailUpdate) return failure()
        saveError?.let { return RamapResult.Error(it) }
        val saved = NotificationSettings(expectedAccountId, shopId, catalog, catalog.clear())
        settings[shopId] = saved
        shopIds -= shopId
        return RamapResult.Success(saved)
    }

    override suspend fun subscribeShop(shopId: String): RamapResult<Unit> {
        subscriptionRequests += shopId
        if (shouldFailUpdate) return failure()
        shopIds += shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun unsubscribeShop(shopId: String): RamapResult<Unit> {
        unsubscriptionRequests += shopId
        if (shouldFailUpdate) return failure()
        shopIds -= shopId
        return RamapResult.Success(Unit)
    }

    private fun failure() = RamapResult.Error(RamapError.Unknown(IllegalStateException("failure")))
}
