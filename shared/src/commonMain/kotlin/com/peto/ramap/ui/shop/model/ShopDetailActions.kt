package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable

@Immutable
data class ShopDetailActions(
    val onDismissRequest: () -> Unit,
    val onRetry: () -> Unit,
    val onMenuRetry: () -> Unit = onRetry,
    val onReviewsRetry: () -> Unit = onRetry,
    val shopActions: ShopDetailShopActions,
    val reviewActions: ShopDetailReviewActions,
    val externalLinkActions: ShopDetailExternalLinkActions,
)
