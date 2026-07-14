package com.peto.ramap.network

import com.peto.ramap.shared.RamapConfig
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging

@OptIn(SupabaseInternal::class)
val supabaseClient =
    createSupabaseClient(
        supabaseUrl = RamapConfig.SUPABASE_URL,
        supabaseKey = RamapConfig.SUPABASE_ANON_KEY,
    ) {
        httpConfig {
            install(Logging) {
                level = LogLevel.INFO
            }
        }
        install(Postgrest)
        install(Auth) {
            scheme = AUTH_DEEPLINK_SCHEME
            host = AUTH_DEEPLINK_HOST
            flowType = FlowType.PKCE
            configurePlatformAuth()
        }
    }

const val AUTH_DEEPLINK_SCHEME = "ramap"
const val AUTH_DEEPLINK_HOST = "auth"
