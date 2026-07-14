package com.peto.ramap.di

import com.peto.ramap.network.NaverReverseGeocoder
import com.peto.ramap.network.supabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import org.koin.dsl.module

val networkModule =
    module {
        single { supabaseClient }
        single {
            HttpClient {
                install(Logging) {
                    level = LogLevel.INFO
                }
            }
        }
        single { NaverReverseGeocoder(get()) }
    }
