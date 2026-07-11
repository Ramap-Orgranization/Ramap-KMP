package com.peto.ramap.data.auth

import io.github.jan.supabase.SupabaseClient

interface KakaoLoginProvider {
    suspend fun signIn(supabaseClient: SupabaseClient)
}
