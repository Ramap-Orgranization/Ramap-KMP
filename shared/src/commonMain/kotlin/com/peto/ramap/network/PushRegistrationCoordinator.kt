package com.peto.ramap.network

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object PushRegistrationCoordinator {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var registrationJob: Job? = null

    fun track(
        identifier: String,
        platform: String,
        targetType: String,
    ) {
        if (identifier.isBlank()) return
        registrationJob?.cancel()
        registrationJob =
            scope.launch {
                observeAuthenticatedSessions {
                    runCatching { registerPushRegistration(identifier, platform, targetType) }
                }
            }
    }

    private suspend fun observeAuthenticatedSessions(onAuthenticated: suspend () -> Unit) {
        supabaseClient.auth.sessionStatus.collectLatest { status ->
            if (status is SessionStatus.Authenticated) onAuthenticated()
        }
    }

    private suspend fun registerPushRegistration(
        identifier: String,
        platform: String,
        targetType: String,
    ) {
        if (identifier.isBlank()) return
        supabaseClient.postgrest.rpc(
            function = "register_push_registration",
            parameters =
                buildJsonObject {
                    put("registration_identifier", identifier)
                    put("device_platform", platform)
                    put("registration_target_type", targetType)
                },
        )
    }
}
