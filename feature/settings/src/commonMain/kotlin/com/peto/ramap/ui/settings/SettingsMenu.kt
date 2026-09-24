package com.peto.ramap.ui.settings

internal enum class SettingsMenu {
    ACCOUNT,
    INFORMATION,
    NOTIFICATION,
    ;

    companion object {
        fun visibleSettingsMenus(isNotificationSupported: Boolean): List<SettingsMenu> =
            if (isNotificationSupported) {
                listOf(ACCOUNT, NOTIFICATION, INFORMATION)
            } else {
                listOf(ACCOUNT, INFORMATION)
            }
    }
}
