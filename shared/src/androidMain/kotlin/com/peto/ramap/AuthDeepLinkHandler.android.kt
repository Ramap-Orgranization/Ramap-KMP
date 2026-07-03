package com.peto.ramap

import android.content.Intent
import com.peto.ramap.network.supabaseClient
import io.github.jan.supabase.auth.handleDeeplinks

fun handleAuthDeepLink(intent: Intent) {
    supabaseClient.handleDeeplinks(intent)
}
