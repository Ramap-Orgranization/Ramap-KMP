package com.peto.ramap

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatformTools
import platform.Foundation.NSURL

fun handleAuthDeepLink(url: NSURL) {
    KoinPlatformTools
        .defaultContext()
        .get()
        .get<SupabaseClient>()
        .handleDeeplinks(url)
}
