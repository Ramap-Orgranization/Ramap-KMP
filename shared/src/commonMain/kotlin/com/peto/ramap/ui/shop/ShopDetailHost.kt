package com.peto.ramap.ui.shop

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.notice.OperatingNoticeBottomSheet
import com.peto.ramap.designsystem.resource.wating.toUiModel
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.lifecycle.ScreenViewModelScope
import com.peto.ramap.ui.shop.component.ShopDetailContent
import com.peto.ramap.ui.shop.menu.ShopMenuRoute
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailExternalLinkActions
import com.peto.ramap.ui.shop.model.ShopDetailSheetConfig
import com.peto.ramap.ui.shop.model.ShopDetailShopActions
import com.peto.ramap.ui.shop.overview.OverviewRoute
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.ReviewsRoute
import org.koin.compose.koinInject

@Composable
fun ShopDetailHost(
    shopId: String,
    onDismiss: () -> Unit,
    onShowOnMap: ((String) -> Unit)?,
    onReviewNavigate: (String) -> Unit,
    onOpenProfile: (String) -> Unit = {},
    onEditReview: (String, String) -> Unit = { _, _ -> },
    isNavigationBarPadded: Boolean = false,
    onEventNavigate: (ShopEvent) -> Unit = {},
    originSource: AnalyticsSource = AnalyticsSource.MAP,
    isUncovered: Boolean = true,
    initialShop: RamenShop? = null,
    onShopViewed: (RamenShop) -> Unit = {},
    onShopLoadFailed: (String) -> Unit = {},
    onLoginGuideRequested: () -> Unit = {},
    onSelectedShopHidden: () -> Unit = onDismiss,
    onOperatingNoticeNavigate: ((OperatingNotice) -> Unit)? = null,
    showReviewsOnOpen: Boolean = false,
    isBackEnabled: Boolean = true,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
) {
    var visible by remember(shopId) { mutableStateOf(true) }
    if (!visible) return
    ScreenViewModelScope(shopId) {
        val owner = requireNotNull(LocalViewModelStoreOwner.current)
        var selectedNotice by remember { mutableStateOf<OperatingNotice?>(null) }
        val clear: () -> Unit = {
            owner.viewModelStore.clear()
            visible = false
        }

        OverviewRoute(
            shopId = shopId,
            initialShop = initialShop,
            originSource = originSource,
            onDismissRequest = {
                clear()
                onDismiss()
            },
            onShopViewed = onShopViewed,
            onShopLoadFailed = onShopLoadFailed,
            onLoginGuideRequested = onLoginGuideRequested,
            onSelectedShopHidden = {
                clear()
                onSelectedShopHidden()
            },
            toastManager = toastManager,
            appSettingsOpener = appSettingsOpener,
            shopShareLinkFactory = shopShareLinkFactory,
            requestNotificationPermission = requestNotificationPermission,
        ) { uiState, overviewActions ->
            ShopMenuRoute(shopId = shopId) { menuState, onMenuRetry ->
                ReviewsRoute(
                    shopId = shopId,
                    isLoggedIn = uiState.isLoggedIn,
                    onReviewNavigate = onReviewNavigate,
                    onOpenProfile = onOpenProfile,
                    onEditReview = onEditReview,
                    onLoginGuideRequested = overviewActions.onLoginGuideRequested,
                    onLoginRequired = onLoginGuideRequested,
                    toastManager = toastManager,
                ) { reviewState, reviewActions ->
                    val displayState =
                        rememberShopDetailDisplayState(
                            shopId = shopId,
                            overview = uiState,
                            menu = menuState,
                            reviews = reviewState,
                        )
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        ShopDetailContent(
                            uiState =
                                ShopDetailContentUiState(
                                    shopId = shopId,
                                    displayState = displayState,
                                    sheetState = uiState.overviewState,
                                    headerState =
                                        HeaderUiState(
                                            waitingSystem = uiState.selectedShop?.let { uiState.shopWaiting[it.id].toUiModel() },
                                            isBookmarked = uiState.selectedShop?.id in uiState.bookmarkedShopIds,
                                            isNotificationEnabled = uiState.selectedShop?.id in uiState.notificationShopIds,
                                            showNotificationActions = NotificationPermissionRequester.isSupported,
                                            isHidden = uiState.selectedShop?.id in uiState.hiddenShopIds,
                                            isAppleMapsAvailable = ExternalUriOpener.isAppleMapsAvailable,
                                        ),
                                    isLoggedIn = uiState.isLoggedIn,
                                    menuState = menuState,
                                    reviewState = reviewState,
                                    overviewHasLoadFailure = uiState.hasLoadFailure,
                                ),
                            config =
                                ShopDetailSheetConfig(
                                    isBackEnabled = isBackEnabled,
                                    maxHeight = maxHeight,
                                    isNavigationBarPadded = isNavigationBarPadded,
                                    showReviewsOnOpen = showReviewsOnOpen,
                                ),
                            actions =
                                ShopDetailActions(
                                    onDismissRequest = overviewActions.onDismissRequest,
                                    onRetry = overviewActions.onRetry,
                                    onMenuRetry = onMenuRetry,
                                    shopActions =
                                        ShopDetailShopActions(
                                            onBookmarkToggled = overviewActions.onBookmarkToggled,
                                            onShopNotificationToggled = overviewActions.onShopNotificationToggled,
                                            onHiddenToggled = overviewActions.onHiddenToggled,
                                            onShopShareClick = overviewActions.onShopShareClick,
                                            onShopMapLinkClick = overviewActions.onShopMapLinkClick,
                                            onEventClick = onEventNavigate,
                                            onOperatingNoticeClick = { selectedNotice = it },
                                            onReportSubmit = overviewActions.onReportSubmit,
                                            onAppleMapsClick = overviewActions.onAppleMapsClick,
                                        ),
                                    reviewActions = reviewActions,
                                    externalLinkActions =
                                        ShopDetailExternalLinkActions(
                                            onWaitingClick = ExternalUriOpener::open,
                                            onExternalLinkClick = ExternalUriOpener::open,
                                            onShowOnMap =
                                                onShowOnMap?.let { showOnMap ->
                                                    { selectedShopId ->
                                                        overviewActions.onDismissRequest()
                                                        showOnMap(selectedShopId)
                                                    }
                                                },
                                        ),
                                ),
                        )
                    }
                }
            }
        }

        selectedNotice?.let { notice ->
            OperatingNoticeBottomSheet(
                notice = notice,
                isSourceUrlSupported = ExternalUriOpener::isSupportedWebUri,
                onSourceClick = ExternalUriOpener::open,
                onShopClick = {
                    if (it != shopId) {
                        if (onOperatingNoticeNavigate != null) onOperatingNoticeNavigate(notice) else onShowOnMap?.invoke(it)
                    }
                    selectedNotice = null
                },
                onDismiss = { selectedNotice = null },
            )
        }
    }
}
