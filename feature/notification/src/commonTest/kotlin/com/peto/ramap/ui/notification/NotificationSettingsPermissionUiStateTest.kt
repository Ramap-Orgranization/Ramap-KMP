package com.peto.ramap.ui.notification

import com.peto.ramap.ui.notification.model.NotificationSettingsPermissionUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationSettingsPermissionUiStateTest {
    @Test
    fun `권한 요청이 거부되면 전체 알림 활성화를 보류한다`() {
        val state = NotificationSettingsPermissionUiState().onEnableRequestResult(false)

        assertTrue(state.isEnablePending)
        assertFalse(state.isGranted)
        assertFalse(state.shouldEnableServerNotifications)
    }

    @Test
    fun `보류 중 시스템 설정에서 권한을 허용하면 서버 알림 활성화가 필요하다`() {
        val state =
            NotificationSettingsPermissionUiState()
                .onEnableRequestResult(false)
                .onResume(true)

        assertTrue(state.shouldEnableServerNotifications)
        assertFalse(state.consumePendingEnable().shouldEnableServerNotifications)
    }

    @Test
    fun `전체 알림을 명시적으로 끄면 보류 중인 활성화를 취소한다`() {
        val state =
            NotificationSettingsPermissionUiState()
                .onEnableRequestResult(false)
                .onDisabled()
                .onResume(true)

        assertFalse(state.isEnablePending)
        assertFalse(state.shouldEnableServerNotifications)
    }

    @Test
    fun `서버 설정과 시스템 권한이 모두 켜진 경우에만 스위치를 켠다`() {
        val granted = NotificationSettingsPermissionUiState(isGranted = true)
        val denied = NotificationSettingsPermissionUiState(isGranted = false)

        assertTrue(granted.isEnabled(isServerEnabled = true))
        assertFalse(granted.isEnabled(isServerEnabled = false))
        assertFalse(denied.isEnabled(isServerEnabled = true))
        assertFalse(denied.isEnabled(isServerEnabled = false))
    }
}
