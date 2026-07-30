package com.peto.ramap.ui.main.my

internal enum class SettingsMenu {
    ACCOUNT,
    INFORMATION,
    NOTIFICATION,
    REPORT,
    HIDDEN_SHOPS,
    SUBSCRIBED_SHOPS,
    BOOKMARKED_SHOPS,
}

internal fun visibleSettingsMenus(isLoggedIn: Boolean): List<SettingsMenu> {
    val commonMenus =
        listOf(
            SettingsMenu.ACCOUNT,
            SettingsMenu.INFORMATION,
            SettingsMenu.REPORT,
        )
    if (!isLoggedIn) return commonMenus

    return listOf(
        SettingsMenu.ACCOUNT,
        SettingsMenu.INFORMATION,
        SettingsMenu.NOTIFICATION,
        SettingsMenu.REPORT,
    ) +
        listOf(
            SettingsMenu.HIDDEN_SHOPS,
            SettingsMenu.SUBSCRIBED_SHOPS,
            SettingsMenu.BOOKMARKED_SHOPS,
        )
}
