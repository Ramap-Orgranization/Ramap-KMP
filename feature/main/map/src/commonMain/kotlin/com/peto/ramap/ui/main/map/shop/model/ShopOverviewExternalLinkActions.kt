package com.peto.ramap.ui.main.map.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.shop.RamenShop

@Immutable
data class ShopOverviewExternalLinkActions(
    val onMapLinkClick: (String) -> Unit,
    val onWaitingClick: (String) -> Unit,
    val onExternalLinkClick: (String) -> Unit,
    val onAppleMapsClick: (RamenShop) -> Unit,
)
