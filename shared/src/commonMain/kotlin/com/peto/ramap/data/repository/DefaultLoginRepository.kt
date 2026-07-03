package com.peto.ramap.data.repository

import com.peto.ramap.data.auth.KakaoOAuthProvider
import com.peto.ramap.domain.repository.LoginRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Supabase Auth를 사용하는 [LoginRepository] 구현체입니다.
 *
 * 카카오 OAuth 호출을 UI 계층에서 분리하고, Supabase 세션 상태를 앱 진입점에 제공합니다.
 */
class DefaultLoginRepository(
    private val supabaseClient: SupabaseClient,
) : LoginRepository {
    /**
     * 앱 내비게이션이 저장/갱신/신규 생성된 세션 상태에 반응할 수 있도록
     * Supabase Auth의 세션 상태를 그대로 노출합니다.
     */
    override val sessionStatus: StateFlow<SessionStatus> = supabaseClient.auth.sessionStatus

    /**
     * Supabase가 로컬 저장소의 기존 세션 복원을 마칠 때까지 기다립니다.
     */
    override suspend fun awaitInitialization() {
        supabaseClient.auth.awaitInitialization()
    }

    /**
     * 현재 Supabase에 인증된 세션이 있는지 반환합니다.
     */
    override fun hasSession(): Boolean = supabaseClient.auth.currentSessionOrNull() != null

    /**
     * Supabase의 카카오 OAuth 플로우를 시작합니다.
     *
     * OAuth 콜백은 앱 redirect URI에 연결된 Android 딥링크 핸들러에서 완료됩니다.
     */
    override suspend fun signInWithKakao() {
        supabaseClient.auth.signInWith(KakaoOAuthProvider)
    }
}
