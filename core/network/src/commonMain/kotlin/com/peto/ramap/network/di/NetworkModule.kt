package com.peto.ramap.network.di

import com.peto.ramap.network.client.importation.ImportationFunctionClient
import com.peto.ramap.network.supabaseClient
import io.github.jan.supabase.SupabaseClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkModule =
    module {
        single { ImportationFunctionClient(get<SupabaseClient>()) }
        single { supabaseClient }
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
        }
    }
