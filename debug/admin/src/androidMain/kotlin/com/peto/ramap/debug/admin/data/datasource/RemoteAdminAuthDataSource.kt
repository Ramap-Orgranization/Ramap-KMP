package com.peto.ramap.debug.admin.data.datasource

import com.peto.ramap.debug.admin.config.AdminConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

internal class RemoteAdminAuthDataSource(
    private val client: SupabaseClient,
) : AdminAuthDataSource {
    override suspend fun signIn() {
        check(AdminConfig.ADMIN_EMAIL.isNotBlank() && AdminConfig.ADMIN_PASSWORD.isNotBlank()) {
            "Debug administrator credentials are not configured"
        }
        client.auth.signInWith(Email) {
            email = AdminConfig.ADMIN_EMAIL
            password = AdminConfig.ADMIN_PASSWORD
        }
    }
}
