package com.peto.ramap.data.datasource.update

import com.peto.ramap.data.model.AppUpdatePolicyResponse
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

internal class RemoteAppUpdatePolicyDataSource(
    private val client: SupabaseClient,
) : AppUpdatePolicyDataSource {
    override suspend fun fetchAppUpdatePolicy(platform: String): AppUpdatePolicyResponse? =
        client
            .from(TABLE_NAME)
            .select {
                filter { eq(COLUMN_PLATFORM, platform) }
                limit(1)
            }.decodeSingleOrNull()

    private companion object {
        const val TABLE_NAME = "app_update_policies"
        const val COLUMN_PLATFORM = "platform"
    }
}
