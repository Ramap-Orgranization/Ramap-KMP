package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.datasource.personalization.NotificationDataSource
import com.peto.ramap.domain.model.notification.NotificationSelection
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.SubscribedShopRepository
import com.peto.ramap.network.execute.invokeRequest

internal class DefaultSubscribedShopRepository(
    private val dataSource: NotificationDataSource,
    private val loginRepository: LoginRepository,
) : SubscribedShopRepository {
    override suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>> =
        invokeRequest {
            val accountId = loginRepository.currentAccountId() ?: return@invokeRequest emptySet()
            dataSource.fetchSubscribedShopIds(accountId)
        }

    override suspend fun fetchNotificationSettings(
        shopId: String,
        expectedAccountId: String,
    ): RamapResult<NotificationSettings> =
        invokeRequest {
            dataSource.fetchSettings(shopId, expectedAccountId).toDomain()
        }

    override suspend fun saveNotificationSettings(
        settings: NotificationSettings,
        selection: NotificationSelection,
        clearAll: Boolean,
    ): RamapResult<NotificationSettings> =
        invokeRequest {
            settings.catalog.selection(selection.ids)
            dataSource
                .saveSettings(
                    shopId = settings.shopId,
                    expectedAccountId = settings.accountId,
                    revision = settings.catalog.revision,
                    selectedIds = selection.ids.mapTo(mutableSetOf()) { it.value },
                    clearAll = clearAll,
                ).toDomain()
        }

    override suspend fun clearNotificationSettings(
        shopId: String,
        expectedAccountId: String,
    ): RamapResult<NotificationSettings> =
        invokeRequest {
            dataSource.clearSettings(shopId, expectedAccountId).toDomain()
        }

    override suspend fun subscribeShop(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.changeEventSubscription(shopId, true) }

    override suspend fun unsubscribeShop(shopId: String): RamapResult<Unit> = invokeRequest { dataSource.changeEventSubscription(shopId, false) }
}
