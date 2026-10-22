package com.peto.ramap.ui.shop.overview.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.report.ShopInformationField
import com.peto.ramap.domain.model.shop.RamenShop
import org.jetbrains.compose.resources.StringResource

@Immutable
data class OverviewRouteActions(
    val onDismissRequest: () -> Unit,
    val onRetry: () -> Unit,
    val onLoginGuideRequested: (StringResource) -> Unit,
    val onBookmarkToggled: (RamenShop) -> Unit,
    val onShopNotificationToggled: (RamenShop) -> Unit,
    val onHiddenToggled: (RamenShop) -> Unit,
    val onShopShareClick: (RamenShop) -> Unit,
    val onShopMapLinkClick: (RamenShop, String) -> Unit,
    val onReportSubmit: (Set<ShopInformationField>, String) -> Unit,
    val onAppleMapsClick: (RamenShop) -> Unit,
)
