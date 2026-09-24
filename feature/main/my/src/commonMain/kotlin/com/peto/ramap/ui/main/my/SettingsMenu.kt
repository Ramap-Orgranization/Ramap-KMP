package com.peto.ramap.ui.main.my

internal enum class SettingsMenu {
    ACCOUNT,
    PROFILE,
    INFORMATION,
    NOTIFICATION,
    REPORT,
    HIDDEN_SHOPS,
    SUBSCRIBED_SHOPS,
    BOOKMARKED_SHOPS,
}

internal fun visibleSettingsMenus(
    isLoggedIn: Boolean,
    isNotificationSupported: Boolean = true,
): List<SettingsMenu> {
    val commonMenus =
        listOf(
            SettingsMenu.ACCOUNT,
            SettingsMenu.INFORMATION,
            SettingsMenu.REPORT,
        )
    if (!isLoggedIn) return commonMenus

    val loggedInMenus =
        listOf(SettingsMenu.PROFILE) + commonMenus +
            listOf(
                SettingsMenu.HIDDEN_SHOPS,
                SettingsMenu.SUBSCRIBED_SHOPS,
                SettingsMenu.BOOKMARKED_SHOPS,
            )
    return if (isNotificationSupported) {
        loggedInMenus.toMutableList().apply { add(3, SettingsMenu.NOTIFICATION) }
    } else {
        loggedInMenus
    }
}
