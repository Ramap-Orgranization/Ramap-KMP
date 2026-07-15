package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.EventNotificationOverride
import com.peto.ramap.domain.repository.NotificationRepository

class FakeNotificationRepository(
    var enabled: Boolean = true,
    val shopIds: MutableSet<String> = mutableSetOf(),
    val eventOverrides: MutableList<EventNotificationOverride> = mutableListOf(),
) : NotificationRepository {
    val enabledUpdates = mutableListOf<Boolean>()
    val shopNotificationUpdates = mutableListOf<Pair<String, Boolean>>()
    val eventNotificationUpdates = mutableListOf<Pair<String, Boolean>>()

    override suspend fun fetchEventNotificationsEnabled() = RamapResult.Success(enabled)

    override suspend fun updateNotificationEnabled(enabled: Boolean): RamapResult<Unit> {
        enabledUpdates += enabled
        this.enabled = enabled
        return RamapResult.Success(Unit)
    }

    override suspend fun fetchSubscribedShopIds() = RamapResult.Success(shopIds.toSet())

    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        shopNotificationUpdates += shopId to enabled
        if (enabled) shopIds += shopId else shopIds -= shopId
        return RamapResult.Success(Unit)
    }

    override suspend fun fetchEventNotificationEnabled(eventId: String) =
        RamapResult.Success(eventOverrides.firstOrNull { it.eventId == eventId }?.enabled ?: false)

    override suspend fun updateEventNotification(
        eventId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        eventNotificationUpdates += eventId to enabled
        eventOverrides.removeAll { it.eventId == eventId }
        eventOverrides += EventNotificationOverride(eventId, enabled)
        return RamapResult.Success(Unit)
    }

    override suspend fun fetchEventOverrides() = RamapResult.Success(eventOverrides.toList())

    override suspend fun clearEventNotificationOverride(eventId: String): RamapResult<Unit> {
        eventOverrides.removeAll { it.eventId == eventId }
        return RamapResult.Success(Unit)
    }
}
