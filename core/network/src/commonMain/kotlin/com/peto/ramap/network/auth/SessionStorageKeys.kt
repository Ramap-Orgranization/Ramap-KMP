package com.peto.ramap.network.auth

import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.createDefaultSettingsKey

@OptIn(SupabaseInternal::class)
internal class SessionStorageKeys(
    supabaseUrl: String,
) {
    val session: String = "${createDefaultSettingsKey(supabaseUrl)}-session"

    companion object {
        const val LEGACY_SESSION = "session"
    }
}
