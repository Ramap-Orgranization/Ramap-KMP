package com.peto.ramap.notification

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 푸시 알림 수신을 위한 Firebase 푸시 대상 등록을 담당
 *
 * 인증 세션이 활성화될 때까지 대기했다가 서버 RPC에 현재 푸시 대상 정보를 저장ㄴ다.
 */
class NotificationRegistry(
    private val client: SupabaseClient,
    private val scope: CoroutineScope,
) {
    private var registrationJob: Job? = null

    /**
     * 푸시 등록을 시작한다.
     *
     * 인증 세션이 이미 활성화되어 있거나 이후에 활성화되면
     * 지정된 [identifier]를 서버에 등록한다.
     *
     * RPC 실패는 앱 동작을 중단시키지 않도록 의도적으로 무시한다. 이후 인증 세션이 다시
     * 활성화되거나 Firebase가 새 식별자를 전달하면 등록을 다시 시도한다.
     *
     * @param identifier Firebase가 발급한 푸시 대상 식별자. Android에서는 현재 FID를 사용한다.
     * @param platform 디바이스 플랫폼 (예: "android", "ios").
     * @param targetType Firebase 발송 대상 유형. 허용값은 "fid" 또는 "token"이며 APNs 토큰 유형이 아니다.
     */
    fun track(
        identifier: String,
        platform: String,
        targetType: String,
    ) {
        if (identifier.isBlank()) return
        registrationJob?.cancel()
        registrationJob =
            scope.launch {
                observeAuthenticatedSessions {
                    runCatching { registerPushRegistration(identifier, platform, targetType) }
                }
            }
    }

    /**
     * 인증 세션 상태를 관찰하다가 [io.github.jan.supabase.auth.status.SessionStatus.Authenticated] 상태가 될 때마다
     * [onAuthenticated]를 실행한다.
     *
     * `collectLatest`를 사용하므로, 새로운 세션 상태가 emit되면 진행 중이던
     * [onAuthenticated] 블록은 취소되고 최신 상태 기준으로 다시 실행된다.
     *
     * @param onAuthenticated 인증된 세션이 감지될 때마다 실행할 suspend 블록.
     */
    private suspend fun observeAuthenticatedSessions(onAuthenticated: suspend () -> Unit) {
        client.auth.sessionStatus.collectLatest { status ->
            if (status is SessionStatus.Authenticated) onAuthenticated()
        }
    }

    /**
     * 서버(Supabase RPC)에 푸시 등록 정보를 저장한다.
     *
     * @param identifier 등록할 Firebase 푸시 대상 식별자. Android에서는 현재 FID를 사용한다.
     * @param platform 디바이스 플랫폼.
     * @param targetType "fid" 또는 "token" 중 하나인 Firebase 발송 대상 유형. APNs 토큰 유형이 아니다.
     * @throws Exception RPC 호출 실패 시 발생하며 [track]의 `runCatching`이 결과를 소비해 외부로 전파하지 않는다.
     */
    private suspend fun registerPushRegistration(
        identifier: String,
        platform: String,
        targetType: String,
    ) {
        if (identifier.isBlank()) return
        client.postgrest.rpc(
            function = RPC_REGISTER_PUSH_REGISTRATION,
            parameters =
                buildJsonObject {
                    put(PARAM_REGISTRATION_IDENTIFIER, identifier)
                    put(PARAM_DEVICE_PLATFORM, platform)
                    put(PARAM_REGISTRATION_TARGET_TYPE, targetType)
                },
        )
    }

    private companion object {
        const val RPC_REGISTER_PUSH_REGISTRATION = "register_push_registration"
        const val PARAM_REGISTRATION_IDENTIFIER = "registration_identifier"
        const val PARAM_DEVICE_PLATFORM = "device_platform"
        const val PARAM_REGISTRATION_TARGET_TYPE = "registration_target_type"
    }
}
