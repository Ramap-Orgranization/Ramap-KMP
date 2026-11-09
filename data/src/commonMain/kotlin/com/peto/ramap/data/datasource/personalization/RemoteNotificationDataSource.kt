package com.peto.ramap.data.datasource.personalization

import com.peto.ramap.data.model.NotificationSettingsResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemoteNotificationDataSource(
    private val client: SupabaseClient,
) : NotificationDataSource {
    override suspend fun fetchSettings(
        shopId: String,
        expectedAccountId: String,
    ): NotificationSettingsResponse =
        client.postgrest
            .rpc(
                "fetch_shop_notification_settings",
                buildJsonObject {
                    put("p_shop_id", shopId)
                    put("p_expected_account_id", expectedAccountId)
                },
            ).decodeAs()

    override suspend fun saveSettings(
        shopId: String,
        expectedAccountId: String,
        revision: Long,
        selectedIds: Set<String>,
        clearAll: Boolean,
    ): NotificationSettingsResponse =
        client.postgrest
            .rpc(
                "save_shop_notification_settings",
                buildJsonObject {
                    put("p_shop_id", shopId)
                    put("p_expected_account_id", expectedAccountId)
                    put("p_catalog_revision", revision)
                    put("p_selected_type_ids", JsonArray(selectedIds.map(::JsonPrimitive)))
                    put("p_clear_all", clearAll)
                },
            ).decodeAs()

    override suspend fun clearSettings(
        shopId: String,
        expectedAccountId: String,
    ): NotificationSettingsResponse =
        client.postgrest
            .rpc(
                "clear_shop_notification_settings",
                buildJsonObject {
                    put("p_shop_id", shopId)
                    put("p_expected_account_id", expectedAccountId)
                },
            ).decodeAs()

    override suspend fun fetchSubscribedShopIds(expectedAccountId: String): Set<String> =
        client.postgrest
            .rpc(
                "fetch_subscribed_shop_ids",
                buildJsonObject {
                    put("p_expected_account_id", expectedAccountId)
                },
            ).decodeAs<List<String>>()
            .toSet()

    override suspend fun changeEventSubscription(
        shopId: String,
        enabled: Boolean,
    ) {
        client.postgrest.rpc(
            "set_shop_event_notification",
            buildJsonObject {
                put("shop_id", shopId)
                put("enabled", enabled)
            },
        )
    }
}
