package com.peto.ramap.designsystem.toast.model

import androidx.compose.runtime.Immutable

@Immutable
data class ToastAction(
    val label: String,
    val onClick: () -> Unit,
)
