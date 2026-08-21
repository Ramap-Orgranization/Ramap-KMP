package com.peto.ramap.data.datasource.notice

import com.peto.ramap.data.model.OperatingNoticeResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

internal class RemoteOperatingNoticeDataSource(
    private val client: SupabaseClient,
) : OperatingNoticeDataSource {
    override suspend fun fetchApprovedOperatingNotices(): List<OperatingNoticeResponse> =
        client
            .from(TABLE_NAME)
            .select()
            .decodeList()

    private companion object {
        const val TABLE_NAME = "shop_operating_notices"
    }
}
