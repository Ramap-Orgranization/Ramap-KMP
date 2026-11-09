package com.peto.ramap.ui.shop.overview.contract

import com.peto.ramap.domain.model.notification.NotificationSelection
import com.peto.ramap.domain.model.notification.NotificationSettings
import com.peto.ramap.domain.model.personalization.Personalization
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.domain.model.shop.ShopOverView
import com.peto.ramap.domain.model.shop.WaitingSystem
import com.peto.ramap.ui.loading.LoadState
import com.peto.ramap.ui.loading.LoadableState
import com.peto.ramap.ui.shop.model.ShopOverviewUiState

data class ShopOverviewState(
    val overviewState: ShopOverviewUiState = ShopOverviewUiState.Closed,
    val shopWaiting: Map<String, WaitingSystem?> = emptyMap(),
    val isLoggedIn: Boolean = false,
    val personalization: Personalization = Personalization(),
    val hasReceivedPersonalization: Boolean = false,
    val hasLoadFailure: Boolean = false,
    val isShopUnavailable: Boolean = false,
    val notificationSettingsShopId: String? = null,
    val notificationSettings: NotificationSettings? = null,
    val notificationDraft: NotificationSelection? = null,
    val shouldClearAllNotificationPreferences: Boolean = false,
    val hasNotificationLoadFailure: Boolean = false,
    val hasNotificationRevisionMismatch: Boolean = false,
    val isNotificationPermissionGranted: Boolean? = null,
    val hasRequestedNotificationPermission: Boolean = false,
    val notificationPermissionGeneration: Long? = null,
    override val loadState: LoadState = LoadState(),
) : LoadableState<ShopOverviewState> {
    val bookmarkedShopIds: Set<String> get() = personalization.bookmarkedShopIds
    val notificationShopIds: Set<String> get() = personalization.notificationShopIds
    val selectedShop: RamenShop? get() =
        when (val sheet = overviewState) {
            is ShopOverviewUiState.Content -> sheet.overview.shop
            is ShopOverviewUiState.Loading -> sheet.shop
            is ShopOverviewUiState.Error -> sheet.shop
            ShopOverviewUiState.Closed -> null
        }
    val shopOverview: ShopOverView? get() = (overviewState as? ShopOverviewUiState.Content)?.overview
    val isLoading: Boolean get() = loadState.isLoading(ShopOverviewLoadKey.Overview)

    val isNotificationSettingsLoading: Boolean get() = loadState.isLoading(ShopOverviewLoadKey.NotificationSettings)
    val isNotificationSettingsSaving: Boolean get() = loadState.isLoading(ShopOverviewLoadKey.NotificationSave)

    override fun withLoadingState(loadState: LoadState): ShopOverviewState = copy(loadState = loadState)
}
