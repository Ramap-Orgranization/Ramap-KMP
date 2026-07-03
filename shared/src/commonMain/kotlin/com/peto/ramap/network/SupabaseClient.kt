package com.peto.ramap.network

import com.peto.ramap.shared.RamapConfig
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import co.touchlab.kermit.Logger as KermitLogger

private val networkLogger = KermitLogger.withTag("RamapNetwork")

@OptIn(SupabaseInternal::class)
val supabaseClient =
    createSupabaseClient(
        supabaseUrl = RamapConfig.SUPABASE_URL,
        supabaseKey = RamapConfig.SUPABASE_ANON_KEY,
    ) {
        install(Postgrest)
        install(Storage)
        install(Auth) {
            scheme = AUTH_DEEPLINK_SCHEME
            host = AUTH_DEEPLINK_HOST
            flowType = FlowType.PKCE
            configurePlatformAuth()
        }

        httpConfig {
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            networkLogger.d { message }
                        }
                    }
                level = LogLevel.BODY
                sanitizeHeader { header ->
                    header.equals(HttpHeaders.Authorization, ignoreCase = true) ||
                        header.equals(SUPABASE_API_KEY_HEADER, ignoreCase = true)
                }
            }
        }
    }

private const val SUPABASE_API_KEY_HEADER = "apikey"
const val AUTH_DEEPLINK_SCHEME = "ramap"
const val AUTH_DEEPLINK_HOST = "auth"
