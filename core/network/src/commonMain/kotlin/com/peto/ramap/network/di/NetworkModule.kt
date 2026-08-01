package com.peto.ramap.network.di

import com.peto.ramap.domain.repository.ReverseGeocoder
import com.peto.ramap.network.SupabaseReverseGeocoder
import com.peto.ramap.network.supabaseClient
import org.koin.dsl.module

val networkModule =
    module {
        single { supabaseClient }
        single<ReverseGeocoder> { SupabaseReverseGeocoder(get()) }
    }
