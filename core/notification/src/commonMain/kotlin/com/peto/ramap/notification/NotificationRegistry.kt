package com.peto.ramap.notification

import com.peto.ramap.domain.model.auth.LoginSessionState
import com.peto.ramap.domain.repository.LoginRepository
import com.peto.ramap.domain.repository.PushRegistrationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 푸시 알림 수신을 위한 Firebase 푸시 대상 등록을 담당
 *
 * 인증 세션이 활성화될 때까지 대기했다가 현재 푸시 대상 정보를 저장한다.
 */
class NotificationRegistry(
    private val loginRepository: LoginRepository,
    private val pushRegistrationRepository: PushRegistrationRepository,
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
                    runCatching { pushRegistrationRepository.register(identifier, platform, targetType) }
                }
            }
    }

    /**
     * 인증 세션 상태를 관찰하다가 [LoginSessionState.AUTHENTICATED] 상태가 될 때마다
     * [onAuthenticated]를 실행한다.
     *
     * `collectLatest`를 사용하므로, 새로운 세션 상태가 emit되면 진행 중이던
     * [onAuthenticated] 블록은 취소되고 최신 상태 기준으로 다시 실행된다.
     *
     * @param onAuthenticated 인증된 세션이 감지될 때마다 실행할 suspend 블록.
     */
    private suspend fun observeAuthenticatedSessions(onAuthenticated: suspend () -> Unit) {
        loginRepository.sessionState.collectLatest { sessionState ->
            if (sessionState == LoginSessionState.AUTHENTICATED) onAuthenticated()
        }
    }
}
