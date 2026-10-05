package com.peto.ramap.fixture

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalConfig
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.createSupabaseClient

@OptIn(SupabaseInternal::class)
suspend fun withSupabaseSessionFixture(test: suspend (SupabaseClient) -> Unit) {
    val client =
        createSupabaseClient("https://example.com", "test-key") {
            install(Auth) {
                minimalConfig()
                autoSetupPlatform = false
            }
        }
    try {
        test(client)
    } finally {
        client.close()
    }
}

fun authenticatedSessionFixture(userId: String = "viewer") =
    SessionStatus.Authenticated(
        UserSession(
            accessToken = "test-access-token",
            refreshToken = "test-refresh-token",
            expiresIn = 3600,
            tokenType = "bearer",
            user = UserInfo(id = userId, aud = "authenticated"),
        ),
    )
