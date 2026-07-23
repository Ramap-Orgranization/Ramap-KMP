package com.peto.ramap.ui.resource

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource

@Immutable
data class UiText(
    val resource: StringResource,
    val arguments: List<Any> = emptyList(),
)
