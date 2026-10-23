package com.peto.ramap

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.mp.KoinPlatformTools

fun handleAuthDeepLink(intent: Intent) {
    KoinPlatformTools
        .defaultContext()
        .get()
        .get<SupabaseClient>()
        .handleDeeplinks(intent)
}
