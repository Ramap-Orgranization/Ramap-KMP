package com.peto.ramap.network

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.ExternalAuthAction

internal actual fun AuthConfig.configurePlatformAuth() {
    defaultExternalAuthAction = ExternalAuthAction.CustomTabs()
}
