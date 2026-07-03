package com.peto.ramap.data.auth

import io.github.jan.supabase.auth.providers.OAuthProvider

/**
 * 카카오 OAuth 로그인을 위한 커스텀 provider 선언입니다.
 *
 * 현재 Supabase Kotlin 버전에서는 Kakao가 ID token 로그인용으로만 미리 정의되어 있어,
 * OAuth 로그인에는 provider 이름을 직접 지정합니다.
 */
data object KakaoOAuthProvider : OAuthProvider() {
    override val name: String = "kakao"
}
