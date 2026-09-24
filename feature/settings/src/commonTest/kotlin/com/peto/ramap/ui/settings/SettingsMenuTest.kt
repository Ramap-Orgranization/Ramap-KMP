package com.peto.ramap.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsMenuTest {
    @Test
    fun `알림 지원 여부에 따라 설정 메뉴를 구성한다`() {
        assertEquals(
            listOf(SettingsMenu.ACCOUNT, SettingsMenu.NOTIFICATION, SettingsMenu.INFORMATION),
            SettingsMenu.visibleSettingsMenus(isLoggedIn = true, isNotificationSupported = true),
        )
        assertEquals(
            listOf(SettingsMenu.ACCOUNT, SettingsMenu.INFORMATION),
            SettingsMenu.visibleSettingsMenus(isLoggedIn = true, isNotificationSupported = false),
        )
        assertEquals(
            listOf(SettingsMenu.ACCOUNT, SettingsMenu.INFORMATION),
            SettingsMenu.visibleSettingsMenus(isLoggedIn = false, isNotificationSupported = true),
        )
    }
}
