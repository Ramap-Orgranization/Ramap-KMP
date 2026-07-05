package com.peto.ramap.designsystem.toast.model

import androidx.compose.runtime.Immutable

@Immutable
data class ToastData(
    val message: String,
    val type: ToastType,
    val durationMills: Long = 2_000L,
    val action: ToastAction? = null,
)
