package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.NotificationSettingsResponse

internal interface NotificationDataSource {
    suspend fun fetchSettings(
        shopId: String,
        expectedAccountId: String,
    ): NotificationSettingsResponse

    suspend fun saveSettings(
        shopId: String,
        expectedAccountId: String,
        revision: Long,
        selectedIds: Set<String>,
        clearAll: Boolean,
    ): NotificationSettingsResponse

    suspend fun clearSettings(
        shopId: String,
        expectedAccountId: String,
    ): NotificationSettingsResponse

    suspend fun fetchSubscribedShopIds(expectedAccountId: String): Set<String>

    suspend fun changeEventSubscription(
        shopId: String,
        enabled: Boolean,
    )
}
