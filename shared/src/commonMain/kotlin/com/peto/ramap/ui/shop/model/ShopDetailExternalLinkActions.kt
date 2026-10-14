package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable

@Immutable
data class ShopDetailExternalLinkActions(
    val onExternalLinkClick: (String) -> Unit,
    val onWaitingClick: (String) -> Unit = {},
    val onShowOnMap: ((String) -> Unit)? = null,
)
