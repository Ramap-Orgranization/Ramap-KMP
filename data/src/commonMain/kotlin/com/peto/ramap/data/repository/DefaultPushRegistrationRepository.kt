package com.peto.ramap.data.repository

import com.peto.ramap.domain.repository.PushRegistrationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class DefaultPushRegistrationRepository(
    private val supabaseClient: SupabaseClient,
) : PushRegistrationRepository {
    override suspend fun register(
        identifier: String,
        platform: String,
        targetType: String,
    ) {
        if (identifier.isBlank()) return
        supabaseClient.postgrest.rpc(
            function = RPC_REGISTER_PUSH_REGISTRATION,
            parameters =
                buildJsonObject {
                    put(PARAM_REGISTRATION_IDENTIFIER, identifier)
                    put(PARAM_DEVICE_PLATFORM, platform)
                    put(PARAM_REGISTRATION_TARGET_TYPE, targetType)
                },
        )
    }

    private companion object {
        const val RPC_REGISTER_PUSH_REGISTRATION = "register_push_registration"
        const val PARAM_REGISTRATION_IDENTIFIER = "registration_identifier"
        const val PARAM_DEVICE_PLATFORM = "device_platform"
        const val PARAM_REGISTRATION_TARGET_TYPE = "registration_target_type"
    }
}
