package com.peto.ramap.di

import com.peto.ramap.network.supabaseClient
import com.peto.ramap.network.NaverReverseGeocoder
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import co.touchlab.kermit.Logger as KermitLogger
import org.koin.dsl.module

val networkModule =
    module {
        single { supabaseClient }
        single {
            HttpClient {
                install(Logging) {
                    logger = object : Logger {
                        override fun log(message: String) {
                            KermitLogger.withTag("RamapNaverGeocoder").d { message }
                        }
                    }
                    level = LogLevel.ALL
                    sanitizeHeader { header ->
                        header.equals("x-ncp-apigw-api-key-id", ignoreCase = true) ||
                            header.equals("x-ncp-apigw-api-key", ignoreCase = true)
                    }
                }
            }
        }
        single { NaverReverseGeocoder(get()) }
    }
