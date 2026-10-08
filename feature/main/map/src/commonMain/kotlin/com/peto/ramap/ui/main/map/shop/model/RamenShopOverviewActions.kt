package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable

@Immutable
data class RamenShopOverviewActions(
    val headerActions: ShopOverviewHeaderActions,
    val externalLinkActions: ShopOverviewExternalLinkActions,
    val reviewActions: ShopOverviewReviewActions,
)
