package com.peto.ramap.debug.admin.data.datasource

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

internal class RemoteAdminAuthDataSource(
    private val client: SupabaseClient,
) : AdminAuthDataSource {
    override suspend fun signIn(
        email: String,
        password: String,
    ) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }
}
