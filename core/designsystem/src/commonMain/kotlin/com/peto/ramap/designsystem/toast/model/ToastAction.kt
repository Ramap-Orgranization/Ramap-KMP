package com.peto.ramap.designsystem.toast.model

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

@Immutable
data class ToastAction(
    val label: StringResource,
    val onClick: () -> Unit = {},
)
