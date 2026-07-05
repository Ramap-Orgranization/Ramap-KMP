package com.peto.ramap.designsystem.toast.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

@Immutable
data class ToastData(
    val message: StringResource,
    val type: ToastType,
    val durationMills: Long = 2_000L,
    val action: ToastAction? = null,
)
