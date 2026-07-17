package com.peto.ramap.data.repository

import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.model.EventNotificationOverrideResponse
import com.peto.ramap.data.model.EventNotificationPreferenceResponse
import com.peto.ramap.data.model.ShopEventNotificationSubscriptionResponse
import com.peto.ramap.domain.repository.NotificationSettingsRepository
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DefaultNotificationSettingsRepository(
    private val client: SupabaseClient,
) : NotificationSettingsRepository {
    override suspend fun isEnabled(): RamapResult<Boolean> =
        invokeRequest {
            client
                .from(PREFERENCE_TABLE)
                .select()
                .decodeSingleOrNull<EventNotificationPreferenceResponse>()
                ?.enabled ?: true
        }

    override suspend fun updateEnabled(enabled: Boolean): RamapResult<Unit> =
        invokeRequest {
            client.postgrest.rpc(
                SET_ENABLED_RPC,
                buildJsonObject { put(ENABLED_PARAMETER, enabled) },
            )
        }

    override suspend fun fetchSubscribedShopIds(): RamapResult<Set<String>> =
        invokeRequest {
            client
                .from(SHOP_SUBSCRIPTION_TABLE)
                .select()
                .decodeList<ShopEventNotificationSubscriptionResponse>()
                .mapTo(mutableSetOf()) { it.shopId }
        }

    override suspend fun updateShopNotification(
        shopId: String,
        enabled: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            client.postgrest.rpc(
                function = SET_SHOP_NOTIFICATION_RPC,
                parameters =
                    buildJsonObject {
                        put(SHOP_ID_PARAMETER, shopId)
                        put(ENABLED_PARAMETER, enabled)
                    },
            )
        }

    override suspend fun isEventNotificationEnabled(eventId: String): RamapResult<Boolean> =
        invokeRequest {
            client.postgrest
                .rpc(
                    function = EVENT_NOTIFICATION_ENABLED_RPC,
                    parameters = buildJsonObject { put(EVENT_ID_PARAMETER, eventId) },
                ).decodeAs<Boolean>()
        }

    override suspend fun updateEventNotification(
        eventId: String,
        enabled: Boolean,
    ): RamapResult<Unit> =
        invokeRequest {
            client.postgrest.rpc(
                function = SET_EVENT_NOTIFICATION_RPC,
                parameters =
                    buildJsonObject {
                        put(EVENT_ID_PARAMETER, eventId)
                        put(ENABLED_PARAMETER, enabled)
                    },
            )
        }

    override suspend fun fetchEventOverrides(): RamapResult<List<com.peto.ramap.domain.model.notification.EventNotificationOverride>> =
        invokeRequest {
            client
                .from(EVENT_OVERRIDE_TABLE)
                .select()
                .decodeList<EventNotificationOverrideResponse>()
                .map(EventNotificationOverrideResponse::toDomain)
        }

    override suspend fun clearEventNotificationOverride(eventId: String): RamapResult<Unit> =
        invokeRequest {
            client.postgrest.rpc(
                function = SET_EVENT_NOTIFICATION_RPC,
                parameters =
                    buildJsonObject {
                        put(EVENT_ID_PARAMETER, eventId)
                        put(ENABLED_PARAMETER, JsonNull)
                    },
            )
        }

    private companion object {
        private const val EVENT_NOTIFICATION_ENABLED_RPC = "is_event_notification_enabled"
        private const val SET_EVENT_NOTIFICATION_RPC = "set_event_notification"
        private const val SET_ENABLED_RPC = "set_event_notifications_enabled"
        private const val SET_SHOP_NOTIFICATION_RPC = "set_shop_event_notification"
        private const val PREFERENCE_TABLE = "user_event_notification_preferences"
        private const val SHOP_SUBSCRIPTION_TABLE = "shop_event_notification_subscriptions"
        private const val EVENT_OVERRIDE_TABLE = "user_event_notification_overrides"
        private const val EVENT_ID_PARAMETER = "event_id"
        private const val SHOP_ID_PARAMETER = "shop_id"
        private const val ENABLED_PARAMETER = "enabled"
    }
}
