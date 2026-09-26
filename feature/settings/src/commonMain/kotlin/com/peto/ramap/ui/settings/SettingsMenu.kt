package com.peto.ramap.ui.settings

internal enum class SettingsMenu {
    ACCOUNT,
    NOTIFICATION,
    OFFICIAL_ACCOUNT,
    CONTACT,
    PRIVACY_POLICY,
    ;

    companion object {
        fun visibleSettingsMenus(
            isLoggedIn: Boolean,
            isNotificationSupported: Boolean,
        ): List<SettingsMenu> =
            buildList {
                if (isLoggedIn) {
                    add(ACCOUNT)
                    if (isNotificationSupported) add(NOTIFICATION)
                }
                add(OFFICIAL_ACCOUNT)
                add(CONTACT)
                add(PRIVACY_POLICY)
            }
    }
}
