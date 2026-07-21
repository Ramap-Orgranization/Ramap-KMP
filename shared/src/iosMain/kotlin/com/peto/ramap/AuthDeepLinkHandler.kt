package com.peto.ramap

import com.peto.ramap.network.supabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import platform.Foundation.NSURL

fun handleAuthDeepLink(url: NSURL) {
    supabaseClient.handleDeeplinks(url)
}
