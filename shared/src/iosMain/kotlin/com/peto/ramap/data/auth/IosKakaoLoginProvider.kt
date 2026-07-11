package com.peto.ramap.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.signInWith

class IosKakaoLoginProvider : KakaoLoginProvider {
    override suspend fun signIn(supabaseClient: SupabaseClient) {
        supabaseClient.auth.signInWith(KakaoOAuthProvider)
    }
}
