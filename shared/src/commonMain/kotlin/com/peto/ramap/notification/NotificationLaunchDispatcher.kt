package com.peto.ramap.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 알림으로 전달된 딥 링크를 검증하고 앱 내 탐색이 처리할 때까지 보관한다.
 *
 * [NotificationDeepLinkParser]가 지원하는 딥 링크만 대기 상태로 등록하며
 * UI는 [pendingDeepLink]를 관찰해 목적지로 이동한 뒤 [consume]으로 처리 완료를 알린다.
 *
 * @param deepLinkParser 알림 딥 링크의 형식과 지원 여부를 검증하는 파서
 */
class NotificationLaunchDispatcher(
    private val deepLinkParser: NotificationDeepLinkParser,
) {
    private val mutablePendingDeepLink = MutableStateFlow<String?>(null)

    /** 처리 대기 중인 유효한 딥 링크. 대기 중인 값이 없으면 `null`이다. */
    val pendingDeepLink = mutablePendingDeepLink.asStateFlow()

    /**
     * 딥 링크를 검증하고 유효한 경우 처리 대기 상태로 등록한다.
     *
     * 지원하지 않거나 형식이 올바르지 않은 값은 [pendingDeepLink]를 변경하지 않고 무시한다.
     *
     * @param deepLink 알림 Intent에서 전달된 원본 딥 링크
     */
    fun dispatch(deepLink: String?) {
        if (deepLinkParser.parse(deepLink) == null) return
        mutablePendingDeepLink.value = deepLink
    }

    /**
     * 현재 대기 중인 딥 링크가 [deepLink]와 일치하면 처리 완료 상태로 변경한다.
     *
     * 다른 딥 링크가 새로 등록된 경우에는 해당 값을 지우지 않는다.
     *
     * @param deepLink 처리를 완료한 딥 링크
     */
    fun consume(deepLink: String) {
        mutablePendingDeepLink.compareAndSet(deepLink, null)
    }
}
