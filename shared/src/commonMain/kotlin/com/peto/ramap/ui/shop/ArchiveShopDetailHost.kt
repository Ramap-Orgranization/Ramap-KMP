package com.peto.ramap.ui.shop

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.peto.ramap.designsystem.notice.OperatingNoticeBottomSheet
import com.peto.ramap.designsystem.resource.wating.toUiModel
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener
import com.peto.ramap.platform.ExternalUriOpener
import com.peto.ramap.platform.NotificationPermissionRequester
import com.peto.ramap.ui.lifecycle.ScreenViewModelScope
import com.peto.ramap.ui.main.map.archive.ArchiveMapViewModel
import com.peto.ramap.ui.main.map.archive.ArchiveShopDetailContent
import com.peto.ramap.ui.main.map.archive.contract.ArchiveMapIntent
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailTab
import com.peto.ramap.ui.main.map.archive.model.ArchiveShopDetailUiModel
import com.peto.ramap.ui.shop.component.ShopDetailBody
import com.peto.ramap.ui.shop.component.ShopDetailBottomSheet
import com.peto.ramap.ui.shop.menu.ShopMenuRoute
import com.peto.ramap.ui.shop.model.ShopDetailActions
import com.peto.ramap.ui.shop.model.ShopDetailContentUiState
import com.peto.ramap.ui.shop.model.ShopDetailExternalLinkActions
import com.peto.ramap.ui.shop.model.ShopDetailShopActions
import com.peto.ramap.ui.shop.overview.OverviewRoute
import com.peto.ramap.ui.shop.overview.model.HeaderUiState
import com.peto.ramap.ui.shop.review.ReviewsRoute
import org.koin.compose.koinInject

@Composable
fun ArchiveShopDetailHost(
    viewModel: ArchiveMapViewModel,
    onEditReview: (String, String) -> Unit,
    onReviewNavigate: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onEventNavigate: (ShopEvent) -> Unit,
    isBackEnabled: Boolean,
    toastManager: ToastManager = koinInject(),
    onOperatingNoticeNavigate: ((OperatingNotice) -> Unit)? = null,
    appSettingsOpener: AppSettingsOpener = koinInject(),
    shopShareLinkFactory: ShopShareLinkFactory = koinInject(),
    requestNotificationPermission: suspend () -> Boolean = NotificationPermissionRequester::request,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val shop = state.shops[state.selectedShopId] ?: return
    val request =
        ArchiveShopDetailUiModel(
            shop = shop,
            reviews = state.selectedReviews,
            isReviewPhotosLoading = state.isReviewPhotosLoading,
            hasReviewPhotoError = state.hasReviewPhotoError,
            reviewPhotoRetryKeys = state.reviewPhotoRetryKeys,
            onRetryReviewPhotos = { viewModel.dispatch(ArchiveMapIntent.RetryReviewPhotos) },
            onReviewPhotoDownloadFailed = { viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadFailed(it)) },
            onReviewPhotoDownloadSucceeded = { viewModel.dispatch(ArchiveMapIntent.ReviewPhotoDownloadSucceeded(it)) },
            deleteTargetReviewId = state.deleteTargetReviewId,
            isDeletingReview = state.isDeletingReview,
            onRequestReviewDeletion = { viewModel.dispatch(ArchiveMapIntent.RequestReviewDeletion(it)) },
            onDismissReviewDeletion = { viewModel.dispatch(ArchiveMapIntent.DismissReviewDeletion) },
            onConfirmReviewDeletion = { viewModel.dispatch(ArchiveMapIntent.ConfirmReviewDeletion) },
            onDismiss = { viewModel.dispatch(ArchiveMapIntent.SelectShop(null)) },
        )
    val shopId = shop.id
    var visible by remember(shopId) { mutableStateOf(true) }
    if (!visible) return

    ScreenViewModelScope(shopId) {
        val owner = requireNotNull(LocalViewModelStoreOwner.current)
        var selectedNotice by remember { mutableStateOf<OperatingNotice?>(null) }
        val dismiss: () -> Unit = {
            owner.viewModelStore.clear()
            visible = false
            request.onDismiss()
        }
        OverviewRoute(
            shopId = shopId,
            initialShop = request.shop,
            onDismissRequest = dismiss,
            onShopViewed = {},
            onShopLoadFailed = {},
            onLoginGuideRequested = {},
            onSelectedShopHidden = dismiss,
            toastManager = toastManager,
            appSettingsOpener = appSettingsOpener,
            shopShareLinkFactory = shopShareLinkFactory,
            requestNotificationPermission = requestNotificationPermission,
        ) { overviewState, overviewActions ->
            ShopMenuRoute(shopId = shopId) { menuState, onMenuRetry ->
                ReviewsRoute(
                    shopId = shopId,
                    isLoggedIn = overviewState.isLoggedIn,
                    onReviewNavigate = onReviewNavigate,
                    onOpenProfile = onOpenProfile,
                    onEditReview = onEditReview,
                    onLoginGuideRequested = overviewActions.onLoginGuideRequested,
                    onLoginRequired = {},
                    toastManager = toastManager,
                ) { reviewState, reviewActions ->
                    val detailState =
                        ShopDetailContentUiState(
                            shopId = shopId,
                            displayState = rememberShopDetailDisplayState(shopId, overviewState, menuState, reviewState),
                            sheetState = overviewState.overviewState,
                            headerState =
                                HeaderUiState(
                                    waitingSystem = overviewState.shopWaiting[shopId].toUiModel(),
                                    isBookmarked = shopId in overviewState.bookmarkedShopIds,
                                    isNotificationEnabled = shopId in overviewState.notificationShopIds,
                                    showNotificationActions = NotificationPermissionRequester.isSupported,
                                    isHidden = shopId in overviewState.hiddenShopIds,
                                    isAppleMapsAvailable = ExternalUriOpener.isAppleMapsAvailable,
                                ),
                            isLoggedIn = overviewState.isLoggedIn,
                            menuState = menuState,
                            reviewState = reviewState,
                            overviewHasLoadFailure = overviewState.hasLoadFailure,
                        )
                    val detailActions =
                        ShopDetailActions(
                            onDismissRequest = dismiss,
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
                                ),
                        )
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        ShopDetailBody(
                            uiState = detailState,
                            actions = detailActions,
                            showDismissButton = false,
                        ) { informationItems, loadMoreState, loadMoreEnabled ->
                            ArchiveShopDetailContent(
                                request = request.copy(onDismiss = dismiss),
                                onEditReview = onEditReview,
                                sheetContent = { listState, tab, content ->
                                    ShopDetailBottomSheet(
                                        shopId = shopId,
                                        listState = listState,
                                        maxHeight = maxHeight,
                                        isBackEnabled = isBackEnabled,
                                        isNavigationBarPadded = false,
                                        onDismissRequest = dismiss,
                                        loadMoreState = loadMoreState,
                                        loadMoreEnabled = tab == ArchiveShopDetailTab.INFORMATION && loadMoreEnabled,
                                        onLoadMore = reviewActions.onLoadMoreReviews,
                                        collapsedHeightFraction = ARCHIVE_COLLAPSED_HEIGHT_FRACTION,
                                        content = content,
                                    )
                                },
                                shopInformationContent = informationItems,
                            )
                        }
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
                    if (it != shopId) onOperatingNoticeNavigate?.invoke(notice)
                    selectedNotice = null
                },
                onDismiss = { selectedNotice = null },
            )
        }
    }
}

private const val ARCHIVE_COLLAPSED_HEIGHT_FRACTION = 0.7f
