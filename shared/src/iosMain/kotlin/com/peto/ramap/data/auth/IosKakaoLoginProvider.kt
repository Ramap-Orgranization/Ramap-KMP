package com.peto.ramap.data.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken

class IosKakaoLoginProvider : KakaoLoginProvider {
    override suspend fun signIn(supabaseClient: SupabaseClient) {
        val token = IosKakaoLoginBridge.login()
        supabaseClient.auth.signInWith(IDToken) {
            idToken = token.idToken
            provider = Kakao
            accessToken = token.accessToken
        }
    }
}
