package com.peto.ramap.ui.shop.overview.model

import androidx.compose.runtime.Immutable

@Immutable
data class OverviewActions(
    val headerActions: HeaderActions,
    val externalLinkActions: ExternalLinkActions,
)
