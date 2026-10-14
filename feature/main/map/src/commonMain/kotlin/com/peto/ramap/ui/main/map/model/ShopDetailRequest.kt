package com.peto.ramap.ui.main.map.model

import com.peto.ramap.analytics.AnalyticsSource
import com.peto.ramap.designsystem.toast.ToastManager
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.shop.RamenShop
import com.peto.ramap.navigation.deeplink.ShopShareLinkFactory
import com.peto.ramap.platform.AppSettingsOpener

class ShopDetailRequest(
    val shopId: String,
    val initialShop: RamenShop?,
    val onDismiss: () -> Unit,
    val onSelectedShopHidden: () -> Unit,
    val onShopLoadFailed: (String) -> Unit,
    val onLoginGuideRequested: () -> Unit,
    val onShopViewed: (RamenShop) -> Unit,
    val onShowOnMap: ((String) -> Unit)?,
    val onReviewNavigate: (String) -> Unit,
    val onOpenProfile: (String) -> Unit,
    val onEditReview: (String, String) -> Unit,
    val onEventNavigate: (ShopEvent) -> Unit,
    val onOperatingNoticeNavigate: (OperatingNotice) -> Unit,
    val originSource: AnalyticsSource,
    val isUncovered: Boolean,
    val isBackEnabled: Boolean,
    val showReviewsOnOpen: Boolean,
    val showRequestedLoadingInSheet: Boolean,
    val toastManager: ToastManager,
    val appSettingsOpener: AppSettingsOpener,
    val shopShareLinkFactory: ShopShareLinkFactory,
    val requestNotificationPermission: suspend () -> Boolean,
)
