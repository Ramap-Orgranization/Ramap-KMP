package com.peto.ramap.data.repository

import co.touchlab.kermit.Logger
import com.peto.ramap.core.result.RamapResult
import com.peto.ramap.data.auth.loginWithApple
import com.peto.ramap.data.auth.loginWithKakao
import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.model.auth.LoginType
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.network.execute.invokeRequest
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Kakao
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Supabase Auth를 사용하는 [LoginRepository] 구현체입니다.
 *
 * 카카오 OAuth 호출을 UI 계층에서 분리하고, 로그인 세션 상태를 앱 진입점에 제공합니다.
 */
internal class DefaultLoginRepository(
    private val supabaseClient: SupabaseClient,
) : LoginRepository {
    private val logger = Logger.withTag("LoginRepository")

    /**
     * 앱이 Supabase 구현을 알지 않고 인증 여부에 반응할 수 있도록 세션 상태를 변환합니다.
     */
    override val sessionState: Flow<LoginSessionState> =
        supabaseClient.auth.sessionStatus.map(::loginSessionState)

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

    override fun currentUserEmail(): String? = supabaseClient.auth.currentUserOrNull()?.email

    /** 플랫폼 카카오 SDK로 로그인한 뒤 Supabase 세션을 생성합니다. */
    override suspend fun signIn(type: LoginType): RamapResult<Unit> {
        val result =
            invokeRequest {
                when (type) {
                    LoginType.KAKAO -> {
                        val token = loginWithKakao()
                        supabaseClient.auth.signInWith(IDToken) {
                            idToken = token.idToken
                            provider = Kakao
                            accessToken = token.accessToken
                        }
                    }
                    LoginType.APPLE -> {
                        val token = loginWithApple()
                        supabaseClient.auth.signInWith(IDToken) {
                            idToken = token.idToken
                            provider = Apple
                            nonce = token.nonce
                        }
                    }
                }
            }

        if (result is RamapResult.Error) {
            logger.e(result.error.cause) {
                "sign-in failed type=${type.name}, cause=${result.error.cause?.message}"
            }
        }
        return result
    }

    override suspend fun signOut(): RamapResult<Unit> =
        invokeRequest {
            unregisterPushRegistrations()
            supabaseClient.auth.signOut()
        }

    override suspend fun deleteAccount(): RamapResult<Unit> =
        invokeRequest {
            supabaseClient.postgrest.rpc(DELETE_CURRENT_USER_RPC)
            supabaseClient.auth.signOut()
        }

    private suspend fun unregisterPushRegistrations() {
        try {
            supabaseClient.postgrest.rpc(UNREGISTER_PUSH_REGISTRATIONS_RPC)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            // 세션 종료는 서버 푸시 정리보다 우선한다.
        }
    }

    private companion object {
        const val DELETE_CURRENT_USER_RPC = "delete_current_user"
        const val UNREGISTER_PUSH_REGISTRATIONS_RPC = "unregister_push_registrations"
    }
}

internal fun loginSessionState(status: SessionStatus): LoginSessionState =
    if (status is SessionStatus.Authenticated) {
        LoginSessionState.AUTHENTICATED
    } else {
        LoginSessionState.NOT_AUTHENTICATED
    }
