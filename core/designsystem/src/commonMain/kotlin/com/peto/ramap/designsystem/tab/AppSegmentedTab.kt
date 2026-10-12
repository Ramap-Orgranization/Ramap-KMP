package com.peto.ramap.designsystem.tab

import androidx.compose.runtime.Immutable

@Immutable
data class AppSegmentedTab(
    val label: String,
    val count: String? = null,
    val hasCountBadge: Boolean = false,
)
