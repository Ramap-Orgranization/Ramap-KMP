package com.peto.ramap.data.datasource.appnotice

import com.peto.ramap.data.model.AppNoticeResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

internal class RemoteAppNoticeDataSource(
    private val client: SupabaseClient,
) : AppNoticeDataSource {
    override suspend fun fetchActiveAppNotice(platform: String): AppNoticeResponse? =
        client
            .from(TABLE_NAME)
            .select {
                filter {
                    eq(COLUMN_PLATFORM, platform)
                    eq(COLUMN_IS_ENABLED, true)
                }
                limit(1)
            }.decodeSingleOrNull()

    private companion object {
        const val TABLE_NAME = "app_notices"
        const val COLUMN_PLATFORM = "platform"
        const val COLUMN_IS_ENABLED = "is_enabled"
    }
}
