package com.peto.ramap.designsystem.component

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

@Immutable
data class LoginTypeUiModel(
    val buttonTitle: StringResource,
    val buttonLogo: DrawableResource,
    val buttonBackground: Color,
    val buttonBorder: Color,
    val buttonTextColor: Color,
)
