package com.peto.ramap.ui.shop.model

import androidx.compose.runtime.Immutable
import com.peto.ramap.ui.shop.review.model.ReviewActions

@Immutable
data class ShopDetailActions(
    val onDismissRequest: () -> Unit,
    val onRetry: () -> Unit,
    val onMenuRetry: () -> Unit = onRetry,
    val shopActions: ShopDetailShopActions,
    val reviewActions: ReviewActions,
    val externalLinkActions: ShopDetailExternalLinkActions,
)
