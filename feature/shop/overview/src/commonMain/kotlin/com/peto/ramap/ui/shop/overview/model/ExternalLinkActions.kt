package com.peto.ramap.ui.shop.overview.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.domain.model.shop.RamenShop

@Immutable
data class ExternalLinkActions(
    val onMapLinkClick: (String) -> Unit,
    val onWaitingClick: (String) -> Unit,
    val onExternalLinkClick: (String) -> Unit,
    val onAppleMapsClick: (RamenShop) -> Unit,
)
