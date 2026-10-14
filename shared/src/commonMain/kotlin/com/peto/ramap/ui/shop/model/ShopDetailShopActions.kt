package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.event.ShopEvent
import com.peto.ramap.domain.model.notice.OperatingNotice
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop

@Immutable
data class ShopDetailShopActions(
    val onBookmarkToggled: (RamenShop) -> Unit,
    val onShopNotificationToggled: (RamenShop) -> Unit,
    val onHiddenToggled: (RamenShop) -> Unit,
    val onShopShareClick: (RamenShop) -> Unit,
    val onShopMapLinkClick: (RamenShop, String) -> Unit,
    val onEventClick: (ShopEvent) -> Unit,
    val onOperatingNoticeClick: (OperatingNotice) -> Unit,
    val onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    val onAppleMapsClick: (RamenShop) -> Unit = {},
)
