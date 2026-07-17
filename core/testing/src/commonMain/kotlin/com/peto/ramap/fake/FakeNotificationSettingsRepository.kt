package com.peto.ramap.fake

import com.peto.ramap.core.result.RamapError
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.domain.model.notification.EventNotificationOverride
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNotificationSettingsRepository(
    var enabled: Boolean = true,
    val shopIds: MutableSet<String> = mutableSetOf(),
    val eventOverrides: MutableList<EventNotificationOverride> = mutableListOf(),
) : NotificationSettingsRepository {
    private val mutableSubscribedShopIds = MutableStateFlow(shopIds.toSet())
    override val subscribedShopIds: StateFlow<Set<String>> = mutableSubscribedShopIds.asStateFlow()

    val enabledUpdates = mutableListOf<Boolean>()
    val shopNotificationUpdates = mutableListOf<Pair<String, Boolean>>()
    val eventNotificationUpdates = mutableListOf<Pair<String, Boolean>>()
    val requestedEventNotificationIds = mutableListOf<String>()
    var fetchEnabledError: RamapError? = null
    var fetchShopIdsError: RamapError? = null
    var fetchEventOverridesError: RamapError? = null
    var shopNotificationError: RamapError? = null

    override suspend fun fetchEventNotificationsEnabled(): RamapResult<Boolean> =
        fetchEnabledError?.let { RamapResult.Error(it) } ?: RamapResult.Success(enabled)

    override suspend fun updateEventNotificationsEnabled(enabled: Boolean): RamapResult<Unit> {
        enabledUpdates += enabled
        this.enabled = enabled
        return RamapResult.Success(Unit)
    }

    override suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>> {
        fetchShopIdsError?.let { return RamapResult.Error(it) }
        mutableSubscribedShopIds.value = shopIds.toSet()
        return RamapResult.Success(shopIds.toSet())
    }

    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        shopNotificationUpdates += shopId to enabled
        shopNotificationError?.let { return RamapResult.Error(it) }
        if (enabled) shopIds += shopId else shopIds -= shopId
        mutableSubscribedShopIds.value = shopIds.toSet()
        return RamapResult.Success(Unit)
    }

    override suspend fun isEventNotificationEnabled(eventId: String): RamapResult<Boolean> {
        requestedEventNotificationIds += eventId
        return RamapResult.Success(eventOverrides.firstOrNull { it.eventId == eventId }?.enabled ?: false)
    }

    override suspend fun updateEventNotification(
        eventId: String,
        enabled: Boolean,
    ): RamapResult<Unit> {
        eventNotificationUpdates += eventId to enabled
        eventOverrides.removeAll { it.eventId == eventId }
        eventOverrides += EventNotificationOverride(eventId, enabled)
        return RamapResult.Success(Unit)
    }

    override suspend fun fetchEventOverrides(): RamapResult<List<EventNotificationOverride>> =
        fetchEventOverridesError?.let { RamapResult.Error(it) } ?: RamapResult.Success(eventOverrides.toList())

    override suspend fun clearEventNotificationOverride(eventId: String): RamapResult<Unit> {
        eventOverrides.removeAll { it.eventId == eventId }
        return RamapResult.Success(Unit)
    }
}
