package com.peto.ramap

import androidx.compose.runtime.Composable
import com.peto.ramap.ui.main.map.model.ShopDetailRequest
import com.peto.ramap.ui.shop.ShopDetailHost

@Composable
internal fun MapShopDetail(request: ShopDetailRequest) {
    ShopDetailHost(
        shopId = request.shopId,
        initialShop = request.initialShop,
        onDismiss = request.onDismiss,
        onSelectedShopHidden = request.onSelectedShopHidden,
        onShopLoadFailed = request.onShopLoadFailed,
        onLoginGuideRequested = request.onLoginGuideRequested,
        onShopViewed = request.onShopViewed,
        onShowOnMap = request.onShowOnMap,
        onReviewNavigate = request.onReviewNavigate,
        onOpenProfile = request.onOpenProfile,
        onEditReview = request.onEditReview,
        onEventNavigate = request.onEventNavigate,
        onOperatingNoticeNavigate = request.onOperatingNoticeNavigate,
        originSource = request.originSource,
        isUncovered = request.isUncovered,
        isBackEnabled = request.isBackEnabled,
        showReviewsOnOpen = request.showReviewsOnOpen,
        showRequestedLoadingInSheet = request.showRequestedLoadingInSheet,
        toastManager = request.toastManager,
        appSettingsOpener = request.appSettingsOpener,
        shopShareLinkFactory = request.shopShareLinkFactory,
        requestNotificationPermission = request.requestNotificationPermission,
    )
}
