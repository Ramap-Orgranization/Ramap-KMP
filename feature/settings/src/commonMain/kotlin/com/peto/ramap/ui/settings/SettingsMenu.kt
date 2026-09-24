package com.peto.ramap.ui.settings

internal enum class SettingsMenu {
    ACCOUNT,
    INFORMATION,
    NOTIFICATION,
}

internal fun visibleSettingsMenus(isNotificationSupported: Boolean): List<SettingsMenu> =
    if (isNotificationSupported) {
        listOf(SettingsMenu.ACCOUNT, SettingsMenu.NOTIFICATION, SettingsMenu.INFORMATION)
    } else {
        listOf(SettingsMenu.ACCOUNT, SettingsMenu.INFORMATION)
    }
