package com.peto.ramap.network.di

import com.peto.ramap.network.NaverReverseGeocoder
import com.peto.ramap.network.ReverseGeocoder
import com.peto.ramap.network.supabaseClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val networkModule =
    module {
        single { supabaseClient }
        single { HttpClient() }
        single<ReverseGeocoder> { NaverReverseGeocoder(get()) }
    }
