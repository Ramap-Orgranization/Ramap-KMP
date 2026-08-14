package com.peto.ramap.ui.main.my

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsMenuTest {
    @Test
    fun `비로그인 상태에서는 인증이 필요한 설정 메뉴를 노출하지 않는다`() {
        assertEquals(
            listOf(
                SettingsMenu.ACCOUNT,
                SettingsMenu.INFORMATION,
                SettingsMenu.REPORT,
            ),
            visibleSettingsMenus(isLoggedIn = false),
        )
    }

    @Test
    fun `로그인 상태에서는 인증이 필요한 설정 메뉴를 기존 순서로 노출한다`() {
        assertEquals(
            listOf(
                SettingsMenu.ACCOUNT,
                SettingsMenu.INFORMATION,
                SettingsMenu.NOTIFICATION,
                SettingsMenu.REPORT,
                SettingsMenu.HIDDEN_SHOPS,
                SettingsMenu.SUBSCRIBED_SHOPS,
                SettingsMenu.BOOKMARKED_SHOPS,
            ),
            visibleSettingsMenus(isLoggedIn = true),
        )
    }

    @Test
    fun `알림을 지원하지 않는 플랫폼에서는 알림 설정 메뉴를 노출하지 않는다`() {
        assertEquals(
            listOf(
                SettingsMenu.ACCOUNT,
                SettingsMenu.INFORMATION,
                SettingsMenu.REPORT,
                SettingsMenu.HIDDEN_SHOPS,
                SettingsMenu.SUBSCRIBED_SHOPS,
                SettingsMenu.BOOKMARKED_SHOPS,
            ),
            visibleSettingsMenus(isLoggedIn = true, isNotificationSupported = false),
        )
    }
}
