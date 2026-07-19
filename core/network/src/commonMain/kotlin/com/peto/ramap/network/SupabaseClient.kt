package com.peto.ramap.network

import com.peto.ramap.network.config.RamapSecrets
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

@OptIn(SupabaseInternal::class)
val supabaseClient =
    createSupabaseClient(
        supabaseUrl = RamapSecrets.supabaseUrl,
        supabaseKey = RamapSecrets.supabaseAnonKey,
    ) {
        install(Postgrest)
        install(Functions)
        install(Auth) {
            scheme = AUTH_DEEPLINK_SCHEME
            host = AUTH_DEEPLINK_HOST
            flowType = FlowType.PKCE
            configurePlatformAuth()
        }
    }

const val AUTH_DEEPLINK_SCHEME = "ramap"
const val AUTH_DEEPLINK_HOST = "auth"
