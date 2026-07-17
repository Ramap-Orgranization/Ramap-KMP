package com.peto.ramap.domain.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import kotlinx.coroutines.flow.StateFlow

interface NotificationSettingsRepository {
    val subscribedShopIds: StateFlow<Set<String>>

    suspend fun fetchEventNotificationsEnabled(): RamapResult<Boolean>

    suspend fun updateEventNotificationsEnabled(enabled: Boolean): RamapResult<Unit>

    suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>>

    suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit>

    suspend fun isEventNotificationEnabled(eventId: String): RamapResult<Boolean>

    suspend fun updateEventNotification(
        eventId: String,
        enabled: Boolean,
    ): RamapResult<Unit>

    suspend fun fetchEventOverrides(): RamapResult<List<EventNotificationOverride>>

    suspend fun clearEventNotificationOverride(eventId: String): RamapResult<Unit>
}
