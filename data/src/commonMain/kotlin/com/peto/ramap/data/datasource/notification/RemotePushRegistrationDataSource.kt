package com.peto.ramap.data.datasource.notification

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal class RemotePushRegistrationDataSource(
    private val client: SupabaseClient,
) : PushRegistrationDataSource {
    override suspend fun register(
        identifier: String,
        platform: String,
        targetType: String,
        expectedAccountId: String,
        supportedProtocolVersion: Int,
    ) {
        client.postgrest.rpc(
            function = "register_push_registration_protocol",
            parameters =
                buildJsonObject {
                    put("registration_identifier", identifier)
                    put("device_platform", platform)
                    put("registration_target_type", targetType)
                    put("expected_account_id", expectedAccountId)
                    put("supported_protocol_version", supportedProtocolVersion)
                },
        )
    }
}
