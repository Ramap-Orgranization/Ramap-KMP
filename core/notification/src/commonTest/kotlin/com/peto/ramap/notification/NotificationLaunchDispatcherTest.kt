package com.peto.ramap.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NotificationLaunchDispatcherTest {
    private val dispatcher = NotificationLaunchDispatcher(NotificationDeepLinkParser())
    private val notices = """{"version":1,"action":"operating_notices"}"""

    @Test
    fun `같은 목적지의 서로 다른 알림은 구별하고 중복 클릭은 무시한다`() {
        dispatcher.dispatch(notices, "delivery-a")
        val first = assertNotNull(dispatcher.pendingDeepLink.value)
        dispatcher.consume(first)
        dispatcher.dispatch(notices, "delivery-a")
        assertNull(dispatcher.pendingDeepLink.value)
        dispatcher.dispatch(notices, "delivery-b")
        assertEquals("delivery-b", dispatcher.pendingDeepLink.value?.notificationId)
        dispatcher.consume(first)
        assertEquals("delivery-b", dispatcher.pendingDeepLink.value?.notificationId)
    }

    @Test
    fun `기존 FCM 메시지 식별자의 구분자도 보존한다`() {
        dispatcher.dispatch(notices, "0:123456789%abcdef")
        assertEquals("0:123456789%abcdef", dispatcher.pendingDeepLink.value?.notificationId)
    }

    @Test
    fun `추가 이동 없음은 이전 대기 탐색을 지우고 재실행하지 않는다`() {
        dispatcher.dispatch(notices, "delivery-a")
        dispatcher.dispatch("""{"version":1,"action":"none"}""", "delivery-b")
        assertNull(dispatcher.pendingDeepLink.value)
        dispatcher.dispatch(notices, "delivery-a")
        assertNull(dispatcher.pendingDeepLink.value)
    }

    @Test
    fun `알 수 없는 명령과 잘못된 식별자는 대기 명령을 바꾸지 않는다`() {
        dispatcher.dispatch(notices, "delivery-a")
        dispatcher.dispatch("""{"version":9,"action":"none"}""", "delivery-b")
        dispatcher.dispatch(notices, "invalid identity")
        assertEquals("delivery-a", dispatcher.pendingDeepLink.value?.notificationId)
    }
}
