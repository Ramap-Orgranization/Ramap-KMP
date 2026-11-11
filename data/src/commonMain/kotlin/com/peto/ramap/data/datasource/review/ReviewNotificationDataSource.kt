package com.peto.ramap.data.datasource.review

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class ReviewNotificationDataSource(
    private val client: SupabaseClient,
) {
    suspend fun fetchEnabled(accountId: String): Boolean =
        client.postgrest
            .rpc(
                "fetch_review_notifications_enabled",
                buildJsonObject { put("p_expected_account_id", accountId) },
            ).decodeAs()

    suspend fun updateEnabled(
        accountId: String,
        enabled: Boolean,
    ): Boolean =
        client.postgrest
            .rpc(
                "set_review_notifications_enabled",
                buildJsonObject {
                    put("p_expected_account_id", accountId)
                    put("p_enabled", enabled)
                },
            ).decodeAs()

    suspend fun updateReviewEnabled(
        reviewId: String,
        enabled: Boolean,
    ): Boolean =
        client.postgrest
            .rpc(
                "set_review_notification_enabled",
                buildJsonObject {
                    put("p_review_id", reviewId)
                    put("p_enabled", enabled)
                },
            ).decodeAs()
}
