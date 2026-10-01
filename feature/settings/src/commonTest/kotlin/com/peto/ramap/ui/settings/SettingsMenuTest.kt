package com.peto.ramap.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsMenuTest {
    @Test
    fun `회원은 계정과 공통 설정 메뉴를 본다`() {
        assertEquals(
            listOf(
                SettingsMenu.ACCOUNT,
                SettingsMenu.NOTIFICATION,
                SettingsMenu.OFFICIAL_ACCOUNT,
                SettingsMenu.CONTACT,
                SettingsMenu.PRIVACY_POLICY,
            ),
            SettingsMenu.visibleSettingsMenus(isLoggedIn = true, isNotificationSupported = true),
        )
        assertEquals(
            listOf(
                SettingsMenu.ACCOUNT,
                SettingsMenu.OFFICIAL_ACCOUNT,
                SettingsMenu.CONTACT,
                SettingsMenu.PRIVACY_POLICY,
            ),
            SettingsMenu.visibleSettingsMenus(isLoggedIn = true, isNotificationSupported = false),
        )
    }

    @Test
    fun `비회원은 공통 설정 메뉴만 본다`() {
        assertEquals(
            listOf(
                SettingsMenu.OFFICIAL_ACCOUNT,
                SettingsMenu.CONTACT,
                SettingsMenu.PRIVACY_POLICY,
            ),
            SettingsMenu.visibleSettingsMenus(isLoggedIn = false, isNotificationSupported = true),
        )
    }
}
