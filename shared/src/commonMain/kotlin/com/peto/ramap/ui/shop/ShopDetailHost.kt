package com.peto.ramap.ui.shop

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.notice.OperatingNoticeBottomSheet
import com.peto.ramap.designsystem.resource.wating.toUiModel
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.designsystem.toast.model.ToastData
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.lifecycle.ScreenViewModelScope
import com.peto.ramap.ui.shop.component.MapInteractionHost
import com.peto.ramap.ui.shop.component.ShopDetailContent
import com.peto.ramap.ui.shop.component.dialog.MapBlockedReviewUnblockDialog
import com.peto.ramap.ui.shop.component.dialog.MapReviewReportDialog
import com.peto.ramap.ui.shop.contract.ShopInteractionEffect
import com.peto.ramap.ui.shop.menu.ShopMenuViewModel
import com.peto.ramap.ui.shop.menu.contract.ShopMenuIntent
import com.peto.ramap.ui.shop.model.ReviewPagingUiState
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailExternalLinkActions
import com.peto.ramap.ui.shop.model.ShopDetailReviewActions
import com.peto.ramap.ui.shop.model.ShopDetailSheetConfig
import com.peto.ramap.ui.shop.model.ShopDetailShopActions
import com.peto.ramap.ui.shop.model.UserContext
import com.peto.ramap.ui.shop.overview.ShopOverviewViewModel
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewIntent
import com.peto.ramap.ui.shop.overview.contract.ShopOverviewSideEffect
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.ShopReviewViewModel
import com.peto.ramap.ui.shop.review.contract.ShopReviewIntent
import com.peto.ramap.ui.shop.review.contract.ShopReviewSideEffect
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

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
    showRequestedLoadingInSheet: Boolean = true,
    toastManager: ToastManager = koinInject(),
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
) {
    var visible by remember(shopId) { mutableStateOf(true) }
    if (!visible) return
    ScreenViewModelScope(shopId) {
        val overview: ShopOverviewViewModel = koinViewModel()
        val menu: ShopMenuViewModel = koinViewModel()
        val reviews: ShopReviewViewModel = koinViewModel()
        val uiState by overview.uiState.collectAsStateWithLifecycle()
        val menuState by menu.uiState.collectAsStateWithLifecycle()
        val reviewState by reviews.uiState.collectAsStateWithLifecycle()
        val owner = requireNotNull(LocalViewModelStoreOwner.current)

        val dismiss: () -> Unit = {
            overview.entryContext.advance()
            owner.viewModelStore.clear()
            visible = false
            onDismiss()
        }

        val currentOnLoginGuideRequested by rememberUpdatedState(onLoginGuideRequested)
        val currentOnShopViewed by rememberUpdatedState(onShopViewed)
        val currentOnHidden by rememberUpdatedState {
            overview.entryContext.advance()
            owner.viewModelStore.clear()
            visible = false
            onSelectedShopHidden()
        }
        val interactions =
            remember(overview, reviews) {
                merge(
                    overview.sideEffect.mapNotNull { effect ->
                        when (effect) {
                            is ShopOverviewSideEffect.Interaction -> {
                                if (effect.effect == ShopInteractionEffect.ShowLoginGuide) currentOnLoginGuideRequested()
                                effect.effect
                            }
                            is ShopOverviewSideEffect.ShopViewed -> {
                                currentOnShopViewed(effect.shop)
                                null
                            }
                            ShopOverviewSideEffect.SelectedShopHidden -> {
                                currentOnHidden()
                                null
                            }
                            is ShopOverviewSideEffect.LoginCompleted -> null
                        }
                    },
                    reviews.sideEffect.map { effect ->
                        when (effect) {
                            ShopReviewSideEffect.ShowLoginGuide -> {
                                currentOnLoginGuideRequested()
                                ShopInteractionEffect.ShowLoginGuide
                            }
                            is ShopReviewSideEffect.ShowToast -> ShopInteractionEffect.ShowToast(ToastData(effect.message, effect.type))
                        }
                    },
                ).buffer(0)
            }
        var selectedNotice by remember { mutableStateOf<OperatingNotice?>(null) }

        LaunchedEffect(shopId) {
            if (initialShop == null) {
                overview.dispatch(ShopOverviewIntent.OnShopIdSelected(shopId))
            } else {
                overview.dispatch(ShopOverviewIntent.OnShopSelected(initialShop))
            }
            menu.dispatch(ShopMenuIntent.OnShopSelected(shopId))
            reviews.dispatch(ShopReviewIntent.OnShopSelected(shopId))
        }

        LaunchedEffect(uiState.hasLoadFailure) {
            if (uiState.hasLoadFailure && uiState.shopDetail == null) onShopLoadFailed(shopId)
        }

        MapInteractionHost(
            sideEffect = interactions,
            isLoggedIn = uiState.isLoggedIn,
            hiddenShopIds = uiState.hiddenShopIds,
            notificationShopIds = uiState.notificationShopIds,
            toastManager = toastManager,
            appSettingsOpener = appSettingsOpener,
            shopShareLinkFactory = shopShareLinkFactory,
            requestNotificationPermission = requestNotificationPermission,
            onNotificationToggled = {
                overview.dispatch(ShopOverviewIntent.OnShopNotificationToggled(it, originSource))
            },
            onLoginTypeSelected = { overview.dispatch(ShopOverviewIntent.OnLoginTypeSelected(it)) },
            onLoginDismissed = { overview.dispatch(ShopOverviewIntent.OnLoginSelectionDismissed) },
        ) { onShopNotificationToggled, onLoginGuideRequested ->
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                if (!showRequestedLoadingInSheet && uiState.isLoading && uiState.selectedShop == null && menuState.menu == null && !reviewState.hasLoaded) {
                    com.peto.ramap.designsystem.indicator
                        .RamenLoadingIndicator(modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
                }
                ShopDetailContent(
                    uiState =
                        ShopDetailContentUiState(
                            sheetState = uiState.shopDetailState,
                            headerState =
                                HeaderUiState(
                                    waitingSystem = uiState.selectedShop?.let { uiState.shopWaiting[it.id].toUiModel() },
                                    isBookmarked = uiState.selectedShop?.id in uiState.bookmarkedShopIds,
                                    isNotificationEnabled = uiState.selectedShop?.id in uiState.notificationShopIds,
                                    showNotificationActions = NotificationPermissionRequester.isSupported,
                                    isHidden = uiState.selectedShop?.id in uiState.hiddenShopIds,
                                    isAppleMapsAvailable = ExternalUriOpener.isAppleMapsAvailable,
                                ),
                            reviewPagingState =
                                ReviewPagingUiState(
                                    isRetryingReviews = reviewState.isLoading,
                                    hasMoreReviews = reviewState.hasMoreReviews,
                                    isLoadingMoreReviews = reviewState.isLoadingMoreReviews,
                                ),
                            userContext =
                                UserContext(
                                    currentUserId = reviewState.currentUserId,
                                    currentProfileIsPublic = reviewState.currentProfileIsPublic,
                                    actingReviewId = reviewState.actingReviewId,
                                    revealedBlockedReviews = reviewState.revealedBlockedReviews,
                                    revealingBlockedReviewId = reviewState.revealingBlockedReviewId,
                                ),
                            isLoggedIn = uiState.isLoggedIn,
                            menuState = menuState,
                            reviewState = reviewState,
                            isShopUnavailable = uiState.isShopUnavailable,
                            overviewHasLoadFailure = uiState.hasLoadFailure,
                            overviewIsLoading = uiState.isLoading,
                        ),
                    config =
                        ShopDetailSheetConfig(
                            isBackEnabled = isBackEnabled,
                            maxHeight = maxHeight,
                            isNavigationBarPadded = isNavigationBarPadded,
                            showRequestedLoadingInSheet = showRequestedLoadingInSheet,
                            showReviewsOnOpen = showReviewsOnOpen,
                        ),
                    actions =
                        ShopDetailActions(
                            onDismissRequest = dismiss,
                            onRetry = { overview.dispatch(ShopOverviewIntent.OnShopDetailRetry) },
                            onMenuRetry = { menu.dispatch(ShopMenuIntent.OnRetry) },
                            onReviewsRetry = { reviews.dispatch(ShopReviewIntent.OnRetry) },
                            shopActions =
                                ShopDetailShopActions(
                                    onBookmarkToggled = {
                                        overview.dispatch(ShopOverviewIntent.OnBookmarkToggled(it, originSource))
                                    },
                                    onShopNotificationToggled = onShopNotificationToggled,
                                    onHiddenToggled = {
                                        overview.dispatch(ShopOverviewIntent.OnHiddenToggled(it, originSource))
                                    },
                                    onShopShareClick = { overview.dispatch(ShopOverviewIntent.OnShopShareClicked(it)) },
                                    onShopMapLinkClick = { shop, provider ->
                                        overview.dispatch(ShopOverviewIntent.OnShopMapLinkClicked(shop, provider))
                                    },
                                    onEventClick = onEventNavigate,
                                    onOperatingNoticeClick = { selectedNotice = it },
                                    onReportSubmit = { wrongFields, description ->
                                        overview.dispatch(ShopOverviewIntent.OnShopReportSubmitted(wrongFields, description))
                                    },
                                    onAppleMapsClick = { shop ->
                                        overview.dispatch(ShopOverviewIntent.OnShopMapLinkClicked(shop, "apple"))
                                        ExternalUriOpener.openAppleMaps(
                                            name = shop.name,
                                            address = shop.address,
                                            latitude = shop.location.lat,
                                            longitude = shop.location.lng,
                                        )
                                    },
                                ),
                            reviewActions =
                                ShopDetailReviewActions(
                                    onReviewsClick = { selectedShopId ->
                                        if (uiState.isLoggedIn) {
                                            onReviewNavigate(selectedShopId)
                                        } else {
                                            onLoginGuideRequested()
                                        }
                                    },
                                    onViewBlockedReview = { reviews.dispatch(ShopReviewIntent.OnBlockedReviewViewRequested(it)) },
                                    onUnblockBlockedUser = { reviews.dispatch(ShopReviewIntent.OnBlockedReviewUnblockRequested(it)) },
                                    onOpenProfile = { userId -> onOpenProfile(userId) },
                                    onReviewLike = { reviews.dispatch(ShopReviewIntent.OnReviewLikeToggled(it)) },
                                    onReviewEdit = { onEditReview(it.shopId, it.id) },
                                    onReviewDelete = { reviews.dispatch(ShopReviewIntent.OnReviewDeleted(it)) },
                                    onReviewReport = { reviews.dispatch(ShopReviewIntent.OnReviewReportRequested(it)) },
                                    onLoadMoreReviews = { reviews.dispatch(ShopReviewIntent.OnLoadMore) },
                                ),
                            externalLinkActions =
                                ShopDetailExternalLinkActions(
                                    onWaitingClick = ExternalUriOpener::open,
                                    onExternalLinkClick = ExternalUriOpener::open,
                                    onShowOnMap =
                                        onShowOnMap?.let { showOnMap ->
                                            { selectedShopId ->
                                                dismiss()
                                                showOnMap(selectedShopId)
                                            }
                                        },
                                ),
                        ),
                )
            }
        }

        MapReviewReportDialog(state = reviewState, onIntent = reviews::dispatch)
        MapBlockedReviewUnblockDialog(state = reviewState, onIntent = reviews::dispatch)

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
